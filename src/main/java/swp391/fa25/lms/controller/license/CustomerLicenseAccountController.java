package swp391.fa25.lms.controller.license;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import swp391.fa25.lms.dto.LicenseAccountDTO;
import swp391.fa25.lms.model.*;
import swp391.fa25.lms.repository.AccountRepository;
import swp391.fa25.lms.repository.LicenseRepository;
import swp391.fa25.lms.repository.PaymentTransactionRepository;
import swp391.fa25.lms.service.CustomerLicenseAccountService;
import swp391.fa25.lms.util.VNPayUtil;

import java.time.LocalDateTime;
import java.util.Collections;

@Controller
@RequestMapping("/customer/license-accounts")
public class CustomerLicenseAccountController {

    private final CustomerLicenseAccountService laService;
    private final AccountRepository accountRepo;
    private final LicenseRepository licenseRepo;
    private final PaymentTransactionRepository paymentTransactionRepo;
    private final VNPayUtil vnpayUtil;

    @Value("${vnpay.returnUrlLicenseRenew:http://localhost:7070/payment/license-renew-return}")
    private String returnUrlLicenseRenew;

    public CustomerLicenseAccountController(CustomerLicenseAccountService laService,
                                            AccountRepository accountRepo,
                                            LicenseRepository licenseRepo,
                                            PaymentTransactionRepository paymentTransactionRepo,
                                            VNPayUtil vnpayUtil) {
        this.laService = laService;
        this.accountRepo = accountRepo;
        this.licenseRepo = licenseRepo;
        this.paymentTransactionRepo = paymentTransactionRepo;
        this.vnpayUtil = vnpayUtil;
    }

    private Long currentAccountId(Authentication auth) {
        return accountRepo.findIdByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy account"));
    }

    @GetMapping
    public String list(Authentication auth,
                       Model model,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       @RequestParam(required = false) String q,
                       @RequestParam(required = false) LicenseAccount.Status status,
                       @RequestParam(required = false) Long toolId,
                       @RequestParam(required = false) Tool.LoginMethod loginMethod,
                       @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime from,
                       @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime to,
                       @RequestParam(defaultValue = "endDate") String sort,
                       @RequestParam(defaultValue = "desc") String dir) {

        Long accountId = currentAccountId(auth);

        model.addAttribute("pageData",
                laService.getMyLicenseAccounts(accountId, q, status, toolId, loginMethod, from, to, page, size, sort, dir));

        model.addAttribute("q", q);
        model.addAttribute("statusStr", status == null ? "" : status.name());
        model.addAttribute("toolId", toolId);
        model.addAttribute("loginMethodStr", loginMethod == null ? "" : loginMethod.name());
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);
        model.addAttribute("size", size);
        model.addAttribute("tools", laService.getAllToolsForFilter());

        return "license/list";
    }

    @GetMapping("/{licenseAccountId}")
    public String view(@PathVariable Long licenseAccountId,
                       Authentication auth,
                       Model model,
                       @ModelAttribute("success") String success,
                       @ModelAttribute("error") String error) {

        Long accountId = currentAccountId(auth);
        LicenseAccount la = laService.getMyLicenseAccountDetail(accountId, licenseAccountId);

        LicenseAccountDTO editDto = new LicenseAccountDTO();
        editDto.setLicenseAccountId(la.getLicenseAccountId());
        editDto.setUsername(la.getUsername() != null ? la.getUsername() : "");
        editDto.setPassword("");

        LicenseAccountDTO renewForm = new LicenseAccountDTO();
        renewForm.setLicenseAccountId(la.getLicenseAccountId());

        prepareViewModel(model, la, editDto, renewForm, false, false, null, success, error);
        return "license/view";
    }

    @PostMapping("/{licenseAccountId}/edit")
    public String editSubmit(@PathVariable Long licenseAccountId,
                             Authentication auth,
                             @Validated(LicenseAccountDTO.CustomerEdit.class)
                             @ModelAttribute("editDto") LicenseAccountDTO editDto,
                             BindingResult br,
                             Model model,
                             RedirectAttributes ra) {

        Long accountId = currentAccountId(auth);
        LicenseAccount la = laService.getMyLicenseAccountDetail(accountId, licenseAccountId);

        LicenseAccountDTO renewForm = new LicenseAccountDTO();
        renewForm.setLicenseAccountId(licenseAccountId);

        if (br.hasErrors()) {
            prepareViewModel(model, la, editDto, renewForm, true, false, null,
                    null, "Vui lòng kiểm tra lại thông tin Edit.");
            return "license/view";
        }

        try {
            laService.editUserPassword(accountId, licenseAccountId, editDto.getUsername(), editDto.getPassword());
            ra.addFlashAttribute("success", "Cập nhật thông tin license thành công!");
            return "redirect:/customer/license-accounts/" + licenseAccountId;
        } catch (IllegalArgumentException ex) {
            prepareViewModel(model, la, editDto, renewForm, true, false, null, null, ex.getMessage());
            return "license/view";
        }
    }

    @PostMapping("/{licenseAccountId}/renew")
    public String renew(@PathVariable Long licenseAccountId,
                        Authentication auth,
                        @Validated(LicenseAccountDTO.CustomerRenew.class)
                        @ModelAttribute("renewForm") LicenseAccountDTO renewForm,
                        BindingResult br,
                        Model model,
                        RedirectAttributes ra,
                        HttpServletRequest request) {

        Long accountId = currentAccountId(auth);
        LicenseAccount la = laService.getMyLicenseAccountDetail(accountId, licenseAccountId);

        LicenseAccountDTO editDto = new LicenseAccountDTO();
        editDto.setLicenseAccountId(licenseAccountId);
        editDto.setUsername(la.getUsername() != null ? la.getUsername() : "");
        editDto.setPassword("");

        if (br.hasErrors()) {
            prepareViewModel(model, la, editDto, renewForm, false, true,
                    "Vui lòng chọn gói gia hạn hợp lệ.", null, null);
            return "license/view";
        }

        try {
            License license = licenseRepo.findById(renewForm.getLicenseId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gói gia hạn!"));

            if (la.getLicense() == null || la.getLicense().getTool() == null ||
                    !license.getTool().getToolId().equals(la.getLicense().getTool().getToolId())) {
                throw new IllegalArgumentException("Gói gia hạn không thuộc tool này!");
            }

            Account account = accountRepo.findById(accountId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản!"));

            PaymentTransaction transaction = new PaymentTransaction();
            transaction.setAccount(account);
            transaction.setTransactionType(PaymentTransaction.TransactionType.LICENSE_RENEWAL);
            transaction.setStatus(PaymentTransaction.TransactionStatus.PENDING);
            transaction.setAmount(license.getPrice() != null
                    ? java.math.BigDecimal.valueOf(license.getPrice().doubleValue())
                    : java.math.BigDecimal.ZERO);
            transaction.setDescription("Gia hạn License Account #" + licenseAccountId + " - " + license.getName());
            transaction.setCreatedAt(LocalDateTime.now());
            transaction.setIpAddress(vnpayUtil.getIpAddress(request));
            transaction.setUserAgent(request.getHeader("User-Agent"));

            String txnRef = String.valueOf(System.currentTimeMillis());
            transaction.setVnpayTxnRef(txnRef);

            transaction = paymentTransactionRepo.save(transaction);

            String orderInfo = "RENEW_LICENSE_" + licenseAccountId + "_" +
                    renewForm.getLicenseId() + "_" + transaction.getTransactionId();

            String vnpayUrl = vnpayUtil.createPaymentUrl(
                    transaction.getAmount().longValue(),
                    orderInfo,
                    txnRef,
                    returnUrlLicenseRenew,
                    request
            );

            if (vnpayUrl == null) throw new IllegalArgumentException("Không tạo được link VNPay.");

            return "redirect:" + vnpayUrl;

        } catch (Exception ex) {
            prepareViewModel(model, la, editDto, renewForm, false, true, ex.getMessage(), null, null);
            return "license/view";
        }
    }

    @GetMapping("/{licenseAccountId}/history")
    public String history(@PathVariable Long licenseAccountId,
                          Authentication auth,
                          Model model) {
        Long accountId = currentAccountId(auth);
        LicenseAccount la = laService.getMyLicenseAccountDetail(accountId, licenseAccountId);
        model.addAttribute("la", la);
        model.addAttribute("history", laService.getHistory(accountId, licenseAccountId));
        model.addAttribute("licenseAccountId", licenseAccountId);
        return "license/history";
    }

    // ✅ USE: nên để Transactional để chắc chắn save + update đồng bộ
    @PostMapping("/{licenseAccountId}/use")
    @Transactional
    public String use(@PathVariable Long licenseAccountId,
                      @RequestParam(required = false) Long orderId,
                      Authentication auth,
                      RedirectAttributes ra) {

        Long accountId = currentAccountId(auth);

        try {
            laService.useLicenseAccount(accountId, licenseAccountId);

            ra.addFlashAttribute("successMsg", "Đã dùng dịch vụ thành công. Hệ thống đã bắt đầu tính thời hạn.");
            ra.addFlashAttribute("success", "Đã dùng dịch vụ thành công. Hệ thống đã bắt đầu tính thời hạn.");

            if (orderId != null) return "redirect:/customer/orders/" + orderId;
            return "redirect:/customer/license-accounts/" + licenseAccountId;

        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("errorMsg", ex.getMessage());
            ra.addFlashAttribute("error", ex.getMessage());

            if (orderId != null) return "redirect:/customer/orders/" + orderId;
            return "redirect:/customer/license-accounts/" + licenseAccountId;
        }
    }

    private void prepareViewModel(Model model,
                                  LicenseAccount la,
                                  LicenseAccountDTO editDto,
                                  LicenseAccountDTO renewForm,
                                  boolean openEditModal,
                                  boolean openRenewModal,
                                  String renewError,
                                  String success,
                                  String error) {

        model.addAttribute("la", la);

        Tool tool = (la.getLicense() != null) ? la.getLicense().getTool() : null;
        model.addAttribute("tool", tool);

        model.addAttribute("renewPackages",
                tool != null ? laService.getRenewPackagesForTool(tool.getToolId()) : Collections.emptyList());

        model.addAttribute("editable",
                tool != null
                        && tool.getLoginMethod() == Tool.LoginMethod.USER_PASSWORD
                        && la.getStatus() == LicenseAccount.Status.ACTIVE);

        model.addAttribute("renewable", la.getStatus() != LicenseAccount.Status.REVOKED);

        model.addAttribute("editDto", editDto);
        model.addAttribute("renewForm", renewForm);

        model.addAttribute("openEditModal", openEditModal);
        model.addAttribute("openRenewModal", openRenewModal);
        model.addAttribute("renewError", renewError);

        if (success != null && !success.isBlank()) model.addAttribute("success", success);
        if (error != null && !error.isBlank()) model.addAttribute("error", error);
    }
}
