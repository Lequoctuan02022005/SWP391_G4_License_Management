package swp391.fa25.lms.service;

import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Role;
import swp391.fa25.lms.repository.AccountRepository;
import swp391.fa25.lms.repository.RoleRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AccountService {
    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);
    private final AccountRepository accountRepo;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private static Scanner sc = new Scanner(System.in);

    @Value("2") // mặc định 2 phút (comment đang ghi 15 nhưng value là 2)
    private int tokenExpiryMinutes;

    @Value("${app.base-url:http://localhost:7070}") // mặc định chạy local
    private String baseUrl;

    // Regex kiểm tra format email
    private static final Pattern EMAIL_REGEX = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final Pattern PASS_REGEX = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*]).{8,}$"
    );

    private static final String SPECIALS = "!@#$%^&*";
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";

    public AccountService(AccountRepository accountRepo, PasswordEncoder passwordEncoder,
                          JavaMailSender mailSender, RoleRepository roleRepository) {
        this.accountRepo = accountRepo;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.roleRepository = roleRepository;
    }

    /**
     * ====================== Đăng ký tài khoản
     * - Validate tất cả fields (không để trống, định dạng, độ dài, ...)
     * - Kiểm tra email chưa được verified trước đó
     * - Mã hóa mật khẩu
     * - Sinh mã xác minh 6 chữ số, set expiry (tokenExpiryMinutes)
     * - Gán default role = CUSTOMER, status = DEACTIVATED, verified = false
     * - Lưu DB và gửi email
     * @param account
     */
    public boolean registerAccount(Account account, BindingResult result) {
        // Trim toàn bộ các field text
        if (account.getEmail() != null) {
            account.setEmail(account.getEmail().trim());
        }
        if (account.getFullName() != null) {
            account.setFullName(account.getFullName().trim());
        }
        if (account.getPhone() != null) {
            account.setPhone(account.getPhone().trim());
        }
        // [NEW] Trim address
        if (account.getAddress() != null) {
            account.setAddress(account.getAddress().trim());
        }

        // =========== Validate cơ bản cho fullName/phone/email =============
        if (account.getFullName() == null || account.getFullName().isBlank()) {
            result.rejectValue("fullName", "error.fullName", "Họ và tên không được để trống.");
        }

        if (account.getPhone() == null || account.getPhone().isBlank()) {
            result.rejectValue("phone", "error.phone", "Số điện thoại không được để trống.");
        } else if (!account.getPhone().matches("^(0[0-9]{9})$")) {
            result.rejectValue("phone", "error.phone", "Số điện thoại không hợp lệ (phải có 10 số, bắt đầu bằng 0).");
        }

        if (account.getEmail() == null || account.getEmail().isBlank()) {
            result.rejectValue("email", "error.email", "Email không được để trống.");
        } else if (!EMAIL_REGEX.matcher(account.getEmail()).matches()) {
            result.rejectValue("email", "error.email", "Định dạng email không hợp lệ.");
        }

        // [NEW] Validate address (nếu bạn muốn cho phép để trống thì không bắt buộc)
        if (account.getAddress() != null && !account.getAddress().isBlank()) {
            if (account.getAddress().matches(".*[@#$%^&*()!].*")) {
                result.rejectValue("address", "error.address", "Địa chỉ không được chứa ký tự đặc biệt (@#$%^&*()!).");
            }
        }

        // Kiểm tra email đã tồn tại chưa
        Optional<Account> existingOpt = accountRepo.findByEmail(account.getEmail());

        if (existingOpt.isPresent()) {
            Account existing = existingOpt.get();

            // Nếu đã ACTIVE -> không cho đăng ký lại
            if (existing.getStatus() == Account.AccountStatus.ACTIVE && Boolean.TRUE.equals(existing.getVerified())) {
                result.rejectValue("email", "error.email", "Email này đã được đăng ký và kích hoạt.");
                return false;
            }

            // Nếu DEACTIVE -> xóa tài khoản cũ để cho phép đăng ký lại
            if (existing.getStatus() == Account.AccountStatus.DEACTIVATED || !existing.getVerified()) {
                accountRepo.delete(existing); // Xóa record cũ (vì chưa xác minh)
            }
        }

        //  Validate mật khẩu
        if (account.getPassword() == null || account.getPassword().isBlank()) {
            result.rejectValue("password", "error.password", "Mật khẩu không được để trống.");
        } else if (account.getPassword().contains(" ")) {
            result.rejectValue("password", "error.password", "Mật khẩu không được chứa khoảng trắng.");
        } else if (!PASS_REGEX.matcher(account.getPassword()).matches()) {
            result.rejectValue("password", "error.password",
                    "Mật khẩu phải có ít nhất 8 ký tự, bao gồm 1 chữ hoa, 1 chữ thường, 1 số và 1 ký tự đặc biệt.");
        }

        // Nếu có lỗi validate thì dừng lại
        if (result.hasErrors()) {
            return false;
        }

        // Mã hóa mật khẩu
        account.setPassword(passwordEncoder.encode(account.getPassword()));

        // Sinh mã xác thực 6 chữ số
        String code = String.format("%06d", new Random().nextInt(999999));
        account.setVerificationCode(code);
        account.setCodeExpiry(LocalDateTime.now().plusMinutes(tokenExpiryMinutes));

        // Các fields default
        account.setStatus(Account.AccountStatus.DEACTIVATED);
        account.setVerified(false);
        account.setCreatedAt(LocalDateTime.now());

        Role role = roleRepository.findByRoleName(Role.RoleName.CUSTOMER)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy role CUSTOMER"));
        account.setRole(role);

        // Lưu Db (bao gồm cả address nếu có)
        accountRepo.save(account);

        // Gửi email
        sendVerificationCode(account, code);

        return true;
    }

    // Gửi mã xác minh sau khi đăng ký thành công
    public void sendVerificationCode(Account account, String code) {
        try {
            String subject = "[LMS] Xác minh tài khoản của bạn";

            // [UPDATED] Nội dung email đẹp hơn, dùng HTML
            String body =
                    "<div style='font-family: Arial, sans-serif; font-size:14px; color:#333;'>"
                            + "<p>Xin chào <b>" + account.getFullName() + "</b>,</p>"
                            + "<p>Cảm ơn bạn đã đăng ký tài khoản trên hệ thống <b>LMS</b>.</p>"
                            + "<p>Mã xác minh của bạn là:</p>"
                            + "<div style='background:#f3f3f3;padding:12px 16px;border-radius:6px;"
                            + "display:inline-block;font-size:20px;font-weight:bold;letter-spacing:4px;'>"
                            + code + "</div>"
                            + "<p style='margin-top:12px;'>Mã này có hiệu lực trong <b>" + tokenExpiryMinutes + " phút</b>."
                            + " Vui lòng không chia sẻ mã này cho bất kỳ ai.</p>"
                            + "<p>Nếu bạn không thực hiện yêu cầu này, hãy bỏ qua email.</p>"
                            + "<p>Trân trọng,<br><b>Đội ngũ LMS</b></p>"
                            + "</div>";

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(account.getEmail());
            helper.setSubject(subject);
            helper.setText(body, true);

            mailSender.send(message);

        } catch (Exception e) {
            logger.error("Lỗi gửi email xác minh", e);
            throw new RuntimeException("Không thể gửi email xác minh. Vui lòng thử lại sau.");
        }
    }

    // Verify Code sau khi dang ky thanh cong
    public void verifyCode(String code) {
        Account acc = accountRepo.findByVerificationCode(code)
                .orElseThrow(() -> new RuntimeException("Mã xác thực không đúng."));

        if (Boolean.TRUE.equals(acc.getVerified()))
            throw new RuntimeException("Tài khoản đã được xác minh trước đó.");

        if (acc.getCodeExpiry().isBefore(LocalDateTime.now()))
            throw new RuntimeException("Mã xác thực đã hết hạn.");

        if (acc.getVerificationCode() == null || !acc.getVerificationCode().equals(code))
            throw new RuntimeException("Mã xác thực không đúng.");

        // Cập nhật trạng thái
        acc.setVerified(true);
        acc.setVerificationCode(null);
        acc.setCodeExpiry(null);
        acc.setStatus(Account.AccountStatus.ACTIVE);
        acc.setUpdatedAt(LocalDateTime.now());

        accountRepo.save(acc);
    }

    // ====================== LOGIN ======================
    public Account login(String email, String password) {

        // 1 Validate input email
        if(email == null || email.trim().isEmpty()) {
            throw new RuntimeException("Email không được để trống");
        }
        if (!EMAIL_REGEX.matcher(email).matches()) {
            throw new RuntimeException("Định dạng email không hợp lệ");
        }
        System.out.println("DEBUG: EMAIL"+email);

        Account account = accountRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại trong hệ thống"));

        // 2 Validate input password
        if (password == null || password.trim().isEmpty()) {
            throw new RuntimeException("Mật khẩu không được để trống");
        }
        if (!passwordEncoder.matches(password, account.getPassword())) {
            throw new RuntimeException("Mật khẩu không đúng");
        }

        //  3 Kiểm tra trạng thái tài khoản
        if (account.getStatus() == Account.AccountStatus.DEACTIVATED) {
            throw new RuntimeException("Tài khoản đã bị vô hiệu hóa");
        }
        if (!Boolean.TRUE.equals(account.getVerified())) {
            throw new RuntimeException("Tài khoản chưa xác minh email");
        }

        return account;
    }

    // ====================== FORGOT PASSWORD ======================
    public void resetPasswordAndSendMail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new RuntimeException("Vui lòng nhập email.");
        }
        if (!EMAIL_REGEX.matcher(email).matches()) {
            throw new RuntimeException("Định dạng email không hợp lệ.");
        }

        Account account = accountRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại trong hệ thống."));

        // Gen password ngẫu nhiên
        String newPassword = generateRandomPassword();

        // Cap nhat DB
        account.setPassword(passwordEncoder.encode(newPassword));
        accountRepo.save(account);

        // Gui mail new password
        sendNewPasswordEmail(account, newPassword);
    }

    public String generateRandomPassword() {
        SecureRandom random = new SecureRandom();

        StringBuilder password = new StringBuilder();
        password.append(UPPER.charAt(random.nextInt(UPPER.length())));
        password.append(LOWER.charAt(random.nextInt(LOWER.length())));
        password.append(DIGITS.charAt(random.nextInt(DIGITS.length())));
        password.append(SPECIALS.charAt(random.nextInt(SPECIALS.length())));

        String words = UPPER + LOWER + DIGITS + SPECIALS;
        for (int i = 4; i < 8; i++) {
            password.append(words.charAt(random.nextInt(words.length())));
        }

        List<Character> chars = password.chars().mapToObj(c -> (char) c).collect(Collectors.toList());
        Collections.shuffle(chars, random);

        StringBuilder finalPassword = new StringBuilder();
        chars.forEach(finalPassword::append);

        return finalPassword.toString();
    }

    private void sendNewPasswordEmail(Account account, String newPassword) {
        try {
            String subject = "[LMS] Mật khẩu mới của bạn";
            String body = "<p>Xin chào <b>" + account.getFullName() + "</b>,</p>"
                    + "<p>Bạn vừa yêu cầu đặt lại mật khẩu.</p>"
                    + "<p><b>Mật khẩu mới của bạn là:</b></p>"
                    + "<div style='background:#f3f3f3;padding:10px;border-radius:5px;"
                    + "font-size:16px;font-weight:bold;text-align:center;'>"
                    + newPassword + "</div>"
                    + "<p>Vui lòng đăng nhập và thay đổi mật khẩu sau khi vào hệ thống.</p>"
                    + "<p>Trân trọng,<br><b>Đội ngũ LMS</b></p>";

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(account.getEmail());
            helper.setSubject(subject);
            helper.setText(body, true);

            mailSender.send(message);

        } catch (Exception e) {
            logger.error("Gửi email mật khẩu mới thất bại", e);
            throw new RuntimeException("Không thể gửi email mật khẩu mới. Vui lòng thử lại sau.");
        }
    }

    // View Profile
    public Account viewProfile(String email){
        return accountRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản email: " + email));
    }

    public Account updateProfile(String email, Account updatedAccount) {
        Account existing = accountRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản email: " + email));

        if (updatedAccount.getFullName() == null || updatedAccount.getFullName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên đăng nhập không được để trống");
        }

        if (updatedAccount.getPhone() != null && !updatedAccount.getPhone().isEmpty()) {
            if (!updatedAccount.getPhone().matches("^(0[0-9]{9})$")) {
                throw new IllegalArgumentException("Số điện thoại không hợp lệ (phải có 10 số, bắt đầu bằng 0)");
            }
        }

        if (updatedAccount.getAddress() != null && !updatedAccount.getAddress().isEmpty()) {
            if (updatedAccount.getAddress().matches(".*[@#$%^&*()!].*")) {
                throw new IllegalArgumentException("Địa chỉ không được chứa ký tự đặc biệt");
            }
        }

        existing.setFullName(updatedAccount.getFullName().trim());
        existing.setPhone(updatedAccount.getPhone());
        existing.setAddress(updatedAccount.getAddress());

        return accountRepo.save(existing);
    }

    public Account registerSeller(String email) {
        Account account = accountRepo.findByEmail(email).orElseThrow(() ->new RuntimeException("Account not found"));

        if(account.getRole().getRoleName() == Role.RoleName.SELLER){
            throw new RuntimeException("Seller is already registered");
        }

        Role sellerRole = roleRepository.findByRoleName(Role.RoleName.SELLER).
                orElseThrow(() ->new RuntimeException("Role not found"));

        account.setRole(sellerRole);
        return accountRepo.save(account);
    }

    @Transactional
    public Account registerGuestToSeller(String email, String fullName) {
        if (email == null || !EMAIL_REGEX.matcher(email).matches()) {
            throw new RuntimeException("Invalid email format");
        }

        Optional<Account> existingOpt = accountRepo.findByEmail(email);

        if (existingOpt.isPresent()) {
            Account existing = existingOpt.get();

            if (existing.getRole() != null && existing.getRole().getRoleName() == Role.RoleName.SELLER) {
                throw new RuntimeException("This email is already registered as a seller");
            }

            Role sellerRole = roleRepository.findByRoleName(Role.RoleName.SELLER)
                    .orElseThrow(() -> new RuntimeException("Role SELLER not found"));
            existing.setRole(sellerRole);

            String otp = String.format("%06d", new Random().nextInt(999999));
            existing.setVerificationCode(otp);
            existing.setCodeExpiry(LocalDateTime.now().plusMinutes(tokenExpiryMinutes));
            existing.setStatus(Account.AccountStatus.DEACTIVATED);
            existing.setVerified(false);

            accountRepo.saveAndFlush(existing);
            sendVerificationCode(existing, otp);
            return existing;
        }

        Account account = new Account();
        account.setEmail(email);
        account.setFullName(fullName);
        account.setCreatedAt(LocalDateTime.now());
        account.setVerified(false);
        account.setStatus(Account.AccountStatus.DEACTIVATED);

        Role sellerRole = roleRepository.findByRoleName(Role.RoleName.SELLER)
                .orElseThrow(() -> new RuntimeException("Role SELLER not found"));
        account.setRole(sellerRole);

        String code = String.format("%06d", new Random().nextInt(999999));
        account.setVerificationCode(code);
        account.setCodeExpiry(LocalDateTime.now().plusMinutes(tokenExpiryMinutes));

        accountRepo.saveAndFlush(account);
        sendVerificationCode(account, code);

        return account;
    }

    //admin_crud account
    public Page<Account> getAll(Pageable pageable) {
        return accountRepo.findAll(pageable);
    }

    public Account getById(Long id) {
        return accountRepo.findById(id).orElse(null);
    }

    public Page<Account> search(String keyword, Pageable pageable) {
        return accountRepo.search(keyword,null, pageable);
    }

    public Account create(Account account) {

        if (accountRepo.existsByEmail(account.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // SET ROLE từ roleId được form submit lên
        if (account.getRole() != null && account.getRole().getRoleId() != null) {
            Role role = roleRepository.findById(account.getRole().getRoleId())
                    .orElseThrow(() -> new RuntimeException("Role not found"));
            account.setRole(role);
        }

        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());

        if (account.getStatus() == null) {
            account.setStatus(Account.AccountStatus.ACTIVE);
        }

        return accountRepo.save(account);
    }

    public Account update(Long id, Account updated) {
        Account acc = accountRepo.findById(id).orElse(null);
        if (acc == null) return null;

        acc.setEmail(updated.getEmail());
        acc.setFullName(updated.getFullName());
        acc.setPhone(updated.getPhone());
        acc.setAddress(updated.getAddress());
        acc.setStatus(updated.getStatus());
        acc.setUpdatedAt(LocalDateTime.now());

        // FIX: Load role FROM DB thay vì gán object rỗng
        if (updated.getRole() != null && updated.getRole().getRoleId() != null) {
            Role role = roleRepository.findById(updated.getRole().getRoleId())
                    .orElse(null);
            acc.setRole(role);
        }

        // KHÔNG update password tại đây
        return accountRepo.save(acc);
    }

    public void delete(Long id) {
        accountRepo.deleteById(id);
    }

    public boolean emailExists(String email) {
        return accountRepo.existsByEmail(email);
    }
}
