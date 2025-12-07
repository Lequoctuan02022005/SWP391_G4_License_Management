package swp391.fa25.lms.controller.sellerpackage;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.SellerPackage;
import swp391.fa25.lms.model.SellerSubscription;
import swp391.fa25.lms.repository.SellerPackageRepository;
import swp391.fa25.lms.repository.SellerSubscriptionRepository;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/seller")
public class SellerPackageController {

    private final SellerPackageRepository packageRepo;
    private final SellerSubscriptionRepository subscriptionRepo;

    // ====================== SHOW RENEW PAGE ======================
    @GetMapping("/renew")
    public String showRenewPage(HttpSession session, Model model) {

        Account seller = (Account) session.getAttribute("loggedInAccount");
        if (seller == null) return "redirect:/login";

        // Lấy gói hiện tại (nếu có)
        SellerSubscription activeSub = subscriptionRepo
                .findByAccountOrderByStartDateDesc(seller)
                .stream()
                .filter(SellerSubscription::isActive)
                .findFirst()
                .orElse(null);

        LocalDateTime expiryDate = activeSub != null ? activeSub.getEndDate() : null;
        model.addAttribute("currentExpiry", expiryDate);

        // Load danh sách gói seller còn hoạt động
        List<SellerPackage> packages = packageRepo.findByStatus(SellerPackage.Status.ACTIVE);
        model.addAttribute("packages", packages);

        return "sellerpackage/renew-package";
    }

    // ====================== HANDLE RENEW ======================
    @PostMapping("/renew")
    public String performRenew(@RequestParam("packageId") int packageId,
                               HttpSession session,
                               Model model) {

        Account seller = (Account) session.getAttribute("loggedInAccount");
        if (seller == null) return "redirect:/login";

        SellerPackage pkg = packageRepo.findById(packageId).orElse(null);
        if (pkg == null) {
            model.addAttribute("error", "Gói không tồn tại!");
            return "redirect:/seller/renew";
        }

        // Tạo subscription mới
        SellerSubscription newSub = new SellerSubscription();
        newSub.setAccount(seller);
        newSub.setSellerPackage(pkg);
        newSub.setStartDate(LocalDateTime.now());
        newSub.setEndDate(LocalDateTime.now().plusMonths(pkg.getDurationInMonths()));
        newSub.setActive(true);

        subscriptionRepo.save(newSub);

        return "redirect:/tools/seller";
    }
}
