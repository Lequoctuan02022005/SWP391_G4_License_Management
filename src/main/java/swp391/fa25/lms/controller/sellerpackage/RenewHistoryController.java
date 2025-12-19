package swp391.fa25.lms.controller.sellerpackage;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.SellerSubscription;
import swp391.fa25.lms.service.SellerSubscriptionService;

import java.time.LocalDate;

@Controller
@RequestMapping("/seller-package/renew-history")
@RequiredArgsConstructor
public class RenewHistoryController {

    private final SellerSubscriptionService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
    public String view(
            HttpSession session,
            Pageable pageable,
            Model model,
            @RequestParam(required = false) String packageName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        Account acc = (Account) session.getAttribute("loggedInAccount");
        if (acc == null) {
            return "redirect:/login";
        }

        boolean isAdmin = false;
        if (acc.getRole().getRoleName().name().equals("ADMIN")) {
            isAdmin = true;
        }

        Page<SellerSubscription> page;

        if (isAdmin) {
            page = service.filter(
                    null,
                    null,
                    packageName,
                    status,
                    fromDate,
                    toDate,
                    pageable
            );
        } else {
            page = service.filter(
                    acc.getFullName(),
                    null,
                    packageName,
                    status,
                    fromDate,
                    toDate,
                    pageable
            );
        }
        // giữ filter
        model.addAttribute("page", page);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("packageName", packageName);
        model.addAttribute("status", status);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);

        return "sellerpackage/renew-history";
    }
}



