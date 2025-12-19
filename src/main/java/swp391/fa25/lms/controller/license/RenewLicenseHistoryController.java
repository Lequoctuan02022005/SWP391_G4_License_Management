package swp391.fa25.lms.controller.license;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.LicenseRenewLog;
import swp391.fa25.lms.service.LicenseRenewLogService;

import java.time.LocalDate;

@Controller
@RequestMapping("/license/renew-history")
@RequiredArgsConstructor
public class RenewLicenseHistoryController {
    private final LicenseRenewLogService service;

    @PreAuthorize("hasAnyRole('CUSTOMER','SELLER')")
    @GetMapping
    public String view(
            HttpSession session,
            Pageable pageable,
            Model model,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            @RequestParam(required = false) Long minAmount,
            @RequestParam(required = false) Long maxAmount
    ) {
        Account acc = (Account) session.getAttribute("loggedInAccount");
        if (acc == null) {
            return "redirect:/login";
        }


        Page<LicenseRenewLog> page;

        String role = acc.getRole().getRoleName().name();

        if (role.equals("CUSTOMER")) {
            page = service.filterForCustomer(
                    acc.getAccountId(),
                    fromDate,
                    toDate,
                    minAmount,
                    maxAmount,
                    pageable
            );
        } else {
            page = service.filterForSeller(
                    acc.getAccountId(),
                    fromDate,
                    toDate,
                    minAmount,
                    maxAmount,
                    pageable
            );
        }
        model.addAttribute("account", acc);
        model.addAttribute("role", acc.getRole().getRoleName().name());
        model.addAttribute("page", page);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("minAmount", minAmount);
        model.addAttribute("maxAmount", maxAmount);

        return "license/renew-license-history";
    }
}