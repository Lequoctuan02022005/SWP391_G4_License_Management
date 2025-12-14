package swp391.fa25.lms.controller.sellerpackage;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.SellerSubscription;
import swp391.fa25.lms.service.SellerSubscriptionService;

@Controller
@RequestMapping("/seller-package/renew-history")
@RequiredArgsConstructor
public class RenewHistoryController {

    private final SellerSubscriptionService service;

    @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
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

        Page<SellerSubscription> page =
                isAdmin
                        ? service.findAll(pageable)
                        : service.findForSeller(acc.getAccountId(), pageable);

        model.addAttribute("page", page);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("type", "SELLER_PACKAGE");

        return "sellerpackage/renew-history";
    }
}



