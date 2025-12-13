package swp391.fa25.lms.service;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import swp391.fa25.lms.dto.LicenseAccountFormDTO;
import swp391.fa25.lms.dto.LicenseRenewDTO;
import swp391.fa25.lms.model.License;
import swp391.fa25.lms.model.LicenseAccount;
import swp391.fa25.lms.model.LicenseRenewLog;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.repository.LicenseAccountRepository;
import swp391.fa25.lms.repository.LicenseRenewLogRepository;
import swp391.fa25.lms.repository.LicenseToolRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
public class LicenseAccountService {

    private final LicenseAccountRepository accRepo;
    private final LicenseToolRepository licenseRepo;
    private final LicenseRenewLogRepository renewLogRepo;

    public LicenseAccountService(LicenseAccountRepository accRepo,
                                 LicenseToolRepository licenseRepo,
                                 LicenseRenewLogRepository renewLogRepo) {
        this.accRepo = accRepo;
        this.licenseRepo = licenseRepo;
        this.renewLogRepo = renewLogRepo;
    }

    // ====== GIỮ NGUYÊN API CŨ ======
    public List<LicenseAccount> getActiveLicenses(long toolId) {
        return accRepo.findByStatusAndLicense_Tool_ToolId(LicenseAccount.Status.ACTIVE, toolId);
    }

    // ====== ADMIN: LIST ======
    public List<LicenseAccount> adminList(Long toolId, String q, LicenseAccount.Status status, Boolean used) {
        List<LicenseAccount> base = (toolId != null)
                ? accRepo.findByLicense_Tool_ToolId(toolId)
                : accRepo.findAll();

        Stream<LicenseAccount> st = base.stream();

        if (status != null) st = st.filter(x -> x.getStatus() == status);
        if (used != null) st = st.filter(x -> used.equals(x.getUsed()));

        if (StringUtils.hasText(q)) {
            String k = q.trim().toLowerCase();
            st = st.filter(x ->
                    (x.getUsername() != null && x.getUsername().toLowerCase().contains(k))
                            || (x.getToken() != null && x.getToken().toLowerCase().contains(k))
                            || (x.getLicense() != null && x.getLicense().getName() != null
                            && x.getLicense().getName().toLowerCase().contains(k))
                            || (x.getLicense() != null && x.getLicense().getTool() != null
                            && x.getLicense().getTool().getToolName() != null
                            && x.getLicense().getTool().getToolName().toLowerCase().contains(k))
            );
        }

        return st.sorted(Comparator.comparing(LicenseAccount::getLicenseAccountId).reversed())
                .toList();
    }

    public List<License> getAllLicenses() {
        return licenseRepo.findAll();
    }

    public LicenseAccount getById(Long id) {
        return accRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy LicenseAccount id=" + id));
    }

    public List<LicenseRenewLog> getRenewLogs(Long licenseAccountId) {
        return renewLogRepo.findByLicenseAccount_LicenseAccountIdOrderByRenewDateDesc(licenseAccountId);
    }

    // ====== ADMIN: CREATE ======
    @Transactional
    public void adminCreate(LicenseAccountFormDTO dto) {
        validateDates(dto.getStartDate(), dto.getEndDate());

        License license = licenseRepo.findById(dto.getLicenseId())
                .orElseThrow(() -> new IllegalArgumentException("License không tồn tại"));

        Tool tool = license.getTool();
        validateByLoginMethod(tool, dto, null);

        LicenseAccount la = new LicenseAccount();
        la.setLicense(license);
        la.setStartDate(dto.getStartDate());
        la.setEndDate(dto.getEndDate());
        la.setStatus(dto.getStatus());
        la.setUsed(Boolean.TRUE.equals(dto.getUsed()));

        applyCredentialsByMethod(tool, dto, la);

        accRepo.save(la);
    }

    // ====== ADMIN: EDIT ======
    @Transactional
    public void adminUpdate(Long id, LicenseAccountFormDTO dto) {
        LicenseAccount existing = getById(id);
        validateDates(dto.getStartDate(), dto.getEndDate());

        License license = licenseRepo.findById(dto.getLicenseId())
                .orElseThrow(() -> new IllegalArgumentException("License không tồn tại"));

        Tool tool = license.getTool();
        validateByLoginMethod(tool, dto, existing);

        existing.setLicense(license);
        existing.setStartDate(dto.getStartDate());
        existing.setEndDate(dto.getEndDate());
        existing.setStatus(dto.getStatus());
        existing.setUsed(Boolean.TRUE.equals(dto.getUsed()));

        applyCredentialsByMethod(tool, dto, existing);

        accRepo.save(existing);
    }

    // ====== ADMIN: RENEW ======
    @Transactional
    public void adminRenew(LicenseRenewDTO dto) {
        LicenseAccount la = getById(dto.getLicenseAccountId());

        LocalDateTime now = LocalDateTime.now();
        if (dto.getNewEndDate().isBefore(now)) {
            throw new IllegalArgumentException("Ngày hết hạn mới phải >= thời điểm hiện tại");
        }
        if (la.getStartDate() != null && dto.getNewEndDate().isBefore(la.getStartDate())) {
            throw new IllegalArgumentException("Ngày hết hạn mới không được < ngày bắt đầu");
        }
        // khuyến nghị: renew phải tăng, không lùi
        if (la.getEndDate() != null && dto.getNewEndDate().isBefore(la.getEndDate())) {
            throw new IllegalArgumentException("Ngày hết hạn mới phải >= ngày hết hạn hiện tại");
        }

        la.setEndDate(dto.getNewEndDate());
        la.setStatus(LicenseAccount.Status.ACTIVE);
        accRepo.save(la);

        LicenseRenewLog log = new LicenseRenewLog();
        log.setLicenseAccount(la);
        log.setRenewDate(now);
        log.setNewEndDate(dto.getNewEndDate());
        log.setAmountPaid(dto.getAmountPaid()); // optional
        log.setTransaction(null);               // admin renew thủ công => có thể null

        renewLogRepo.save(log);
    }

    // ====== VALIDATE ======
    private void validateDates(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) throw new IllegalArgumentException("Thiếu ngày bắt đầu/kết thúc");
        if (end.isBefore(start)) throw new IllegalArgumentException("Ngày kết thúc phải >= ngày bắt đầu");
    }

    private String loginMethodOf(Tool tool) {
        if (tool == null || tool.getLoginMethod() == null) return "";
        return String.valueOf(tool.getLoginMethod()).toUpperCase();
    }

    private boolean isTokenMethod(Tool tool) {
        return "TOKEN".equalsIgnoreCase(loginMethodOf(tool));
    }

    private boolean isUserPassMethod(Tool tool) {
        return "USER_PASSWORD".equalsIgnoreCase(loginMethodOf(tool));
    }

    private void validateByLoginMethod(Tool tool, LicenseAccountFormDTO dto, LicenseAccount editing) {
        if (tool == null) throw new IllegalArgumentException("License chưa gắn Tool");

        if (isTokenMethod(tool)) {
            if (!StringUtils.hasText(dto.getToken()))
                throw new IllegalArgumentException("Tool login_method=TOKEN => Token không được trống");

            String token = dto.getToken().trim();
            boolean exists = accRepo.existsByToken(token);
            if (exists) {
                // edit: cho phép giữ token cũ
                if (editing == null || editing.getToken() == null || !token.equals(editing.getToken())) {
                    throw new IllegalArgumentException("Token đã tồn tại, vui lòng chọn token khác");
                }
            }
        }

        if (isUserPassMethod(tool)) {
            if (!StringUtils.hasText(dto.getUsername()) || !StringUtils.hasText(dto.getPassword()))
                throw new IllegalArgumentException("Tool login_method=USER_PASSWORD => Username/Password không được trống");
        }

        if (!isTokenMethod(tool) && !isUserPassMethod(tool)) {
            throw new IllegalArgumentException("Login method của Tool không hợp lệ (chỉ TOKEN hoặc USER_PASSWORD)");
        }
    }

    private void applyCredentialsByMethod(Tool tool, LicenseAccountFormDTO dto, LicenseAccount target) {
        if (isTokenMethod(tool)) {
            target.setToken(dto.getToken().trim());
            target.setUsername(null);
            target.setPassword(null);
        } else {
            target.setUsername(dto.getUsername().trim());
            target.setPassword(dto.getPassword().trim());
            target.setToken(null);
        }
    }
}
