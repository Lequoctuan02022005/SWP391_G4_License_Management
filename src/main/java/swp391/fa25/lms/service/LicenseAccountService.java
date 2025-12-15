package swp391.fa25.lms.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    @Transactional(readOnly = true)
    public List<LicenseAccount> getActiveLicenses(Long toolId) {
        if (toolId == null) return List.of();
        return accRepo.findByStatusAndLicense_Tool_ToolId(LicenseAccount.Status.ACTIVE, toolId);
    }

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    public List<License> getAllLicenses() {
        return licenseRepo.findAll();
    }

    @Transactional(readOnly = true)
    public LicenseAccount getById(Long id) {
        return accRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy LicenseAccount id=" + id));
    }

    @Transactional(readOnly = true)
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
        validateByLoginMethod(tool, dto, null); // create

        LicenseAccount la = new LicenseAccount();
        la.setLicense(license);
        la.setStartDate(dto.getStartDate());
        la.setEndDate(dto.getEndDate());
        la.setStatus(dto.getStatus());
        la.setUsed(Boolean.TRUE.equals(dto.getUsed()));

        applyCredentialsByMethod(tool, dto, la, true); // create

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
        validateByLoginMethod(tool, dto, existing); // edit

        existing.setLicense(license);
        existing.setStartDate(dto.getStartDate());
        existing.setEndDate(dto.getEndDate());
        existing.setStatus(dto.getStatus());
        existing.setUsed(Boolean.TRUE.equals(dto.getUsed()));

        applyCredentialsByMethod(tool, dto, existing, false); // edit

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
        log.setAmountPaid(dto.getAmountPaid());
        log.setTransaction(null);

        renewLogRepo.save(log);
    }

    // ====== VALIDATE ======
    private void validateDates(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) throw new IllegalArgumentException("Thiếu ngày bắt đầu/kết thúc");
        if (end.isBefore(start)) throw new IllegalArgumentException("Ngày kết thúc phải >= ngày bắt đầu");
    }

    private boolean isTokenMethod(Tool tool) {
        return tool != null && tool.getLoginMethod() == Tool.LoginMethod.TOKEN;
    }

    private boolean isUserPassMethod(Tool tool) {
        return tool != null && tool.getLoginMethod() == Tool.LoginMethod.USER_PASSWORD;
    }

    private void validateByLoginMethod(Tool tool, LicenseAccountFormDTO dto, LicenseAccount editing) {
        if (tool == null) throw new IllegalArgumentException("License chưa gắn Tool");

        if (isTokenMethod(tool)) {
            if (!StringUtils.hasText(dto.getToken()))
                throw new IllegalArgumentException("Tool login_method=TOKEN => Token không được trống");

            String token = dto.getToken().trim();
            boolean exists = accRepo.existsByToken(token);
            if (exists) {
                if (editing == null || editing.getToken() == null || !token.equals(editing.getToken())) {
                    throw new IllegalArgumentException("Token đã tồn tại, vui lòng chọn token khác");
                }
            }
            return;
        }

        if (isUserPassMethod(tool)) {
            if (!StringUtils.hasText(dto.getUsername()))
                throw new IllegalArgumentException("Username không được trống");

            // CREATE: bắt buộc password
            if (editing == null && !StringUtils.hasText(dto.getPassword()))
                throw new IllegalArgumentException("Password không được trống");

            // EDIT: password cho phép trống (không đổi)
            return;
        }

        throw new IllegalArgumentException("Login method của Tool không hợp lệ (chỉ TOKEN hoặc USER_PASSWORD)");
    }

    private void applyCredentialsByMethod(Tool tool, LicenseAccountFormDTO dto, LicenseAccount target, boolean isCreate) {
        if (isTokenMethod(tool)) {
            target.setToken(dto.getToken().trim());
            target.setUsername(null);
            target.setPassword(null);
            return;
        }

        // USER_PASSWORD
        target.setUsername(dto.getUsername() == null ? null : dto.getUsername().trim());
        target.setToken(null);

        if (isCreate) {
            target.setPassword(dto.getPassword() == null ? null : dto.getPassword().trim());
        } else {
            // EDIT: chỉ set nếu người dùng nhập password mới
            if (StringUtils.hasText(dto.getPassword())) {
                target.setPassword(dto.getPassword().trim());
            }
        }
    }
}
