package swp391.fa25.lms.controller.system;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import swp391.fa25.lms.dto.LicenseAccountFormDTO;
import swp391.fa25.lms.dto.LicenseRenewDTO;
import swp391.fa25.lms.model.LicenseAccount;
import swp391.fa25.lms.service.LicenseAccountService;

@Controller
@RequestMapping("/system-admin/license-accounts")
public class SystemAdminLicenseAccountController {

    private final LicenseAccountService licenseAccountService;

    public SystemAdminLicenseAccountController(LicenseAccountService licenseAccountService) {
        this.licenseAccountService = licenseAccountService;
    }

    @GetMapping
    public String list(
            @RequestParam(required = false) Long toolId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) LicenseAccount.Status status,
            @RequestParam(required = false) Boolean used,
            Model model
    ) {
        model.addAttribute("items", licenseAccountService.adminList(toolId, q, status, used));
        model.addAttribute("toolId", toolId);
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("used", used);

        return "admin/list";
    }

    @GetMapping("/create")
    public String showCreate(Model model) {
        model.addAttribute("form", new LicenseAccountFormDTO());
        model.addAttribute("licenses", licenseAccountService.getAllLicenses());
        model.addAttribute("mode", "create");
        model.addAttribute("statuses", LicenseAccount.Status.values());

        return "admin/form";
    }

    @PostMapping("/create")
    public String create(
            @Valid @ModelAttribute("form") LicenseAccountFormDTO form,
            BindingResult br,
            Model model,
            RedirectAttributes ra
    ) {
        model.addAttribute("licenses", licenseAccountService.getAllLicenses());
        model.addAttribute("mode", "create");
        model.addAttribute("statuses", LicenseAccount.Status.values());

        if (br.hasErrors()) {
            return "admin/form";
        }

        try {
            licenseAccountService.adminCreate(form);
            ra.addFlashAttribute("success", "Tạo License Account thành công");
            return "redirect:/system-admin/license-accounts";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "admin/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEdit(@PathVariable Long id, Model model) {
        LicenseAccount la = licenseAccountService.getById(id);

        LicenseAccountFormDTO form = new LicenseAccountFormDTO();
        form.setLicenseAccountId(la.getLicenseAccountId());
        form.setLicenseId(la.getLicense() != null ? la.getLicense().getLicenseId() : null);
        form.setUsername(la.getUsername());
        form.setPassword(la.getPassword());
        form.setToken(la.getToken());
        form.setStartDate(la.getStartDate());
        form.setEndDate(la.getEndDate());
        form.setStatus(la.getStatus());
        form.setUsed(la.getUsed());

        model.addAttribute("form", form);
        model.addAttribute("licenses", licenseAccountService.getAllLicenses());
        model.addAttribute("mode", "edit");
        model.addAttribute("id", id);
        model.addAttribute("statuses", LicenseAccount.Status.values());

        return "admin/form";
    }

    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") LicenseAccountFormDTO form,
            BindingResult br,
            Model model,
            RedirectAttributes ra
    ) {
        model.addAttribute("licenses", licenseAccountService.getAllLicenses());
        model.addAttribute("mode", "edit");
        model.addAttribute("id", id);
        model.addAttribute("statuses", LicenseAccount.Status.values());

        if (br.hasErrors()) {
            return "admin/form";
        }

        try {
            licenseAccountService.adminUpdate(id, form);
            ra.addFlashAttribute("success", "Cập nhật License Account thành công");
            return "redirect:/system-admin/license-accounts";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "admin/form";
        }
    }


    @GetMapping("/{id}/renew")
    public String showRenew(@PathVariable Long id, Model model) {
        LicenseAccount la = licenseAccountService.getById(id);

        LicenseRenewDTO renew = new LicenseRenewDTO();
        renew.setLicenseAccountId(id);

        model.addAttribute("la", la);
        model.addAttribute("renew", renew);
        model.addAttribute("logs", licenseAccountService.getRenewLogs(id));

        return "admin/renew";
    }

    @PostMapping("/{id}/renew")
    public String renew(
            @PathVariable Long id,
            @Valid @ModelAttribute("renew") LicenseRenewDTO renew,
            BindingResult br,
            Model model,
            RedirectAttributes ra
    ) {
        LicenseAccount la = licenseAccountService.getById(id);

        model.addAttribute("la", la);
        model.addAttribute("logs", licenseAccountService.getRenewLogs(id));

        if (br.hasErrors()) {
            return "admin/renew";
        }

        try {
            renew.setLicenseAccountId(id);
            licenseAccountService.adminRenew(renew);

            ra.addFlashAttribute("success", "Gia hạn thành công");
            return "redirect:/system-admin/license-accounts/" + id + "/renew";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "admin/renew";
        }
    }
}
