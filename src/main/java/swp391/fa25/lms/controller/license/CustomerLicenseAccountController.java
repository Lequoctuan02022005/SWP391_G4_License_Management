package swp391.fa25.lms.controller.license;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import swp391.fa25.lms.dto.LicenseAccountDTO;
import swp391.fa25.lms.model.*;
import swp391.fa25.lms.repository.AccountRepository;
import swp391.fa25.lms.service.CustomerLicenseAccountService;

import java.time.LocalDateTime;
import java.util.Collections;

@Controller
@RequestMapping("/customer/license-accounts")
public class CustomerLicenseAccountController {

    private final CustomerLicenseAccountService laService;
    private final AccountRepository accountRepo;

    public CustomerLicenseAccountController(CustomerLicenseAccountService laService,
                                            AccountRepository accountRepo) {
        this.laService = laService;
        this.accountRepo = accountRepo;
    }

    private Long currentAccountId(Authentication auth) {
        String email = auth.getName();
        Account acc = accountRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy account của user đang đăng nhập"));
        return acc.getAccountId();
    }

    // LIST + FILTER + SEARCH + SORT + PAGING
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
                       @RequestParam(defaultValue = "desc") String dir
    ) {
        Long accountId = currentAccountId(auth);

        model.addAttribute("pageData",
                laService.getMyLicenseAccounts(accountId, q, status, toolId, loginMethod, from, to, page, size, sort, dir));

        // đúng biến theo HTML (templates/license/list.html)
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

    // VIEW (from orderId)
    @GetMapping("/order/{orderId}")
    public String viewFromOrder(@PathVariable Long orderId,
                                Authentication auth,
                                Model model) {
        Long accountId = currentAccountId(auth);
        LicenseAccount la = laService.getMyLicenseAccountByOrder(accountId, orderId);
        prepareViewModel(model, la, new LicenseAccountDTO(), new LicenseAccountDTO(), false, false, null, null, null);
        return "license/view";
    }

    // VIEW (detail)
    @GetMapping("/{licenseAccountId}")
    public String view(@PathVariable Long licenseAccountId,
                       Authentication auth,
                       Model model,
                       @ModelAttribute("successMsg") String successMsg,
                       @ModelAttribute("errorMsg") String errorMsg) {
        Long accountId = currentAccountId(auth);
        LicenseAccount la = laService.getMyLicenseAccountDetail(accountId, licenseAccountId);

        LicenseAccountDTO editDto = new LicenseAccountDTO();
        editDto.setLicenseAccountId(la.getLicenseAccountId());
        editDto.setUsername(la.getUsername() != null ? la.getUsername() : "");
        editDto.setPassword("");

        LicenseAccountDTO renewForm = new LicenseAccountDTO();
        renewForm.setLicenseAccountId(la.getLicenseAccountId());

        prepareViewModel(model, la, editDto, renewForm, false, false, null, successMsg, errorMsg);
        return "license/view";
    }

    // EDIT POST (modal)
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
            ra.addFlashAttribute("successMsg", "MSG16: License information updated successfully.");
            return "redirect:/customer/license-accounts/" + licenseAccountId;
        } catch (IllegalArgumentException ex) {
            prepareViewModel(model, la, editDto, renewForm, true, false, null, null, ex.getMessage());
            return "license/view";
        }
    }

    // RENEW POST (modal)
    @PostMapping("/{licenseAccountId}/renew")
    public String renew(@PathVariable Long licenseAccountId,
                        Authentication auth,
                        @Validated(LicenseAccountDTO.CustomerRenew.class)
                        @ModelAttribute("renewForm") LicenseAccountDTO renewForm,
                        BindingResult br,
                        Model model,
                        RedirectAttributes ra) {

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
            laService.renew(accountId, licenseAccountId, renewForm.getLicenseId());
            ra.addFlashAttribute("successMsg", "MSG18: License renewed successfully!");
            return "redirect:/customer/license-accounts/" + licenseAccountId;
        } catch (IllegalArgumentException ex) {
            prepareViewModel(model, la, editDto, renewForm, false, true,
                    ex.getMessage(), null, null);
            return "license/view";
        }
    }

    // HISTORY
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

    // ===== shared prepare =====
    private void prepareViewModel(Model model,
                                  LicenseAccount la,
                                  LicenseAccountDTO editDto,
                                  LicenseAccountDTO renewForm,
                                  boolean openEditModal,
                                  boolean openRenewModal,
                                  String renewError,
                                  String successMsg,
                                  String errorMsg) {
        model.addAttribute("la", la);

        Tool tool = (la.getLicense() != null) ? la.getLicense().getTool() : null;
        model.addAttribute("tool", tool);

        if (tool != null) {
            model.addAttribute("renewPackages", laService.getRenewPackagesForTool(tool.getToolId()));
        } else {
            model.addAttribute("renewPackages", Collections.emptyList());
        }

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

        if (successMsg != null && !successMsg.isBlank()) model.addAttribute("successMsg", successMsg);
        if (errorMsg != null && !errorMsg.isBlank()) model.addAttribute("errorMsg", errorMsg);
    }
}
