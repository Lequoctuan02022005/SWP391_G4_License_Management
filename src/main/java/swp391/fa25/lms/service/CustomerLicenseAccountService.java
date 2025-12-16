package swp391.fa25.lms.service;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.fa25.lms.model.*;
import swp391.fa25.lms.repository.LicenseAccountRepository;
import swp391.fa25.lms.repository.LicenseRenewLogRepository;
import swp391.fa25.lms.repository.LicenseRepository;
import swp391.fa25.lms.repository.ToolRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class CustomerLicenseAccountService {

    private final LicenseAccountRepository laRepo;
    private final LicenseRepository licenseRepo;
    private final LicenseRenewLogRepository renewLogRepo;
    private final ToolRepository toolRepo;

    public CustomerLicenseAccountService(LicenseAccountRepository laRepo,
                                         LicenseRepository licenseRepo,
                                         LicenseRenewLogRepository renewLogRepo,
                                         ToolRepository toolRepo) {
        this.laRepo = laRepo;
        this.licenseRepo = licenseRepo;
        this.renewLogRepo = renewLogRepo;
        this.toolRepo = toolRepo;
    }

    // tools for filter (list page)
    public List<Tool> getAllToolsForFilter() {
        return toolRepo.findAll(Sort.by(Sort.Direction.ASC, "toolName"));
    }

    // map sort key -> JPA property path (LicenseAccount)
    private String mapSort(String sort) {
        if (sort == null || sort.isBlank()) return "endDate";
        return switch (sort) {
            case "toolName" -> "license.tool.toolName";
            case "licenseName" -> "license.name";
            case "price" -> "license.price";
            case "status" -> "status";
            case "startDate" -> "startDate";
            case "endDate" -> "endDate";
            case "orderId" -> "order.orderId";
            default -> "endDate";
        };
    }

    private Sort.Direction mapDir(String dir) {
        return "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
    }

    public Page<LicenseAccount> getMyLicenseAccounts(Long accountId,
                                                     String q,
                                                     LicenseAccount.Status status,
                                                     Long toolId,
                                                     Tool.LoginMethod loginMethod,
                                                     LocalDateTime from,
                                                     LocalDateTime to,
                                                     int page, int size,
                                                     String sort, String dir) {

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 5), 50),
                Sort.by(mapDir(dir), mapSort(sort))
        );

        return laRepo.findMyLicenseAccounts(accountId, q, status, toolId, loginMethod, from, to, pageable);
    }

    public LicenseAccount getMyLicenseAccountDetail(Long accountId, Long licenseAccountId) {
        LicenseAccount la = laRepo.findByLicenseAccountIdAndOrder_Account_AccountId(licenseAccountId, accountId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy License Account hoặc không thuộc về bạn"));

        // auto mark expired (cẩn thận: side effect khi view)
        if (la.getEndDate() != null && la.getStatus() == LicenseAccount.Status.ACTIVE) {
            if (la.getEndDate().isBefore(LocalDateTime.now())) {
                la.setStatus(LicenseAccount.Status.EXPIRED);
                laRepo.save(la);
            }
        }
        return la;
    }

    public LicenseAccount getMyLicenseAccountByOrder(Long accountId, Long orderId) {
        return laRepo.findByOrder_OrderIdAndOrder_Account_AccountId(orderId, accountId)
                .orElseThrow(() -> new IllegalArgumentException("Không có License Account cho order này (hoặc không thuộc về bạn)"));
    }

    public List<License> getRenewPackagesForTool(Long toolId) {
        return licenseRepo.findByTool_ToolIdOrderByDurationDaysAsc(toolId);
    }

    // CUSTOMER: EDIT (USER_PASSWORD)
    @Transactional
    public void editUserPassword(Long accountId, Long licenseAccountId, String username, String password) {
        LicenseAccount la = getMyLicenseAccountDetail(accountId, licenseAccountId);

        Tool tool = (la.getLicense() != null) ? la.getLicense().getTool() : null;
        if (tool == null) throw new IllegalArgumentException("Tool không tồn tại");

        if (tool.getLoginMethod() != Tool.LoginMethod.USER_PASSWORD) {
            throw new IllegalArgumentException("Tool loginMethod=TOKEN không cho phép chỉnh sửa thông tin đăng nhập");
        }
        if (la.getStatus() != LicenseAccount.Status.ACTIVE) {
            throw new IllegalArgumentException("Chỉ License ACTIVE mới được chỉnh sửa");
        }

        la.setUsername(username == null ? null : username.trim());
        la.setPassword(password == null ? null : password.trim()); // tuỳ bạn có encode hay không
        laRepo.save(la);
    }

    // CUSTOMER: RENEW
    @Transactional
    public void renew(Long accountId, Long licenseAccountId, Long licenseId) {
        LicenseAccount la = getMyLicenseAccountDetail(accountId, licenseAccountId);

        if (la.getStatus() == LicenseAccount.Status.REVOKED) {
            throw new IllegalArgumentException("License đã bị REVOKED, không thể gia hạn");
        }

        License pack = licenseRepo.findById(licenseId)
                .orElseThrow(() -> new IllegalArgumentException("Gói license không tồn tại"));

        // gói renew phải cùng Tool
        if (la.getLicense() == null || la.getLicense().getTool() == null ||
                pack.getTool() == null ||
                !Objects.equals(pack.getTool().getToolId(), la.getLicense().getTool().getToolId())) {
            throw new IllegalArgumentException("Gói gia hạn không thuộc tool này");
        }

        Integer days = (pack.getDurationDays() != null) ? pack.getDurationDays() : 0;
        if (days <= 0) throw new IllegalArgumentException("Gói gia hạn không hợp lệ (durationDays)");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oldEnd = la.getEndDate();

        LocalDateTime base = (oldEnd == null || oldEnd.isBefore(now)) ? now : oldEnd;
        LocalDateTime newEnd = base.plusDays(days);

        la.setEndDate(newEnd);
        la.setStatus(LicenseAccount.Status.ACTIVE);
        la.setLicense(pack);
        laRepo.save(la);

        LicenseRenewLog log = new LicenseRenewLog();
        log.setLicenseAccount(la);
        log.setRenewDate(now);
        log.setNewEndDate(newEnd);
        log.setAmountPaid(pack.getPrice() != null ? BigDecimal.valueOf(pack.getPrice()) : BigDecimal.ZERO);

        renewLogRepo.save(log);
    }

    public java.util.List<LicenseRenewLog> getHistory(Long accountId, Long licenseAccountId) {
        getMyLicenseAccountDetail(accountId, licenseAccountId);
        return renewLogRepo.findByLicenseAccount_LicenseAccountIdOrderByRenewDateDesc(licenseAccountId);
    }
}
