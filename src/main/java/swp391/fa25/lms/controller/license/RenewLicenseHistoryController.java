package swp391.fa25.lms.controller.license;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.net.openssl.ciphers.Authentication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.LicenseRenewLog;
import swp391.fa25.lms.service.LicenseRenewLogService;

@Controller
@RequestMapping("/license/renew-history")
@RequiredArgsConstructor
public class RenewLicenseHistoryController {
 private final LicenseRenewLogService service;

@PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
@GetMapping
public String view(
        HttpSession session,
        Pageable pageable,
        Model model
) {
    Account acc = (Account) session.getAttribute("account");
    if (acc == null) {
        return "redirect:/login";
    }
    boolean isAdmin = acc.getRole().getRoleName().name().equals("ADMIN");

    Page<LicenseRenewLog> page =
            isAdmin
                    ? service.findAll(pageable)
                    : service.findForCustomer(acc.getAccountId(), pageable);

    model.addAttribute("page", page);
    model.addAttribute("isAdmin", isAdmin);
    model.addAttribute("type", "LICENSE");

    return "license/renew-license-history";
}
}