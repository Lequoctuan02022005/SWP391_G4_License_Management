package swp391.fa25.lms.controller.sellerpackage;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.PaymentTransaction;
import swp391.fa25.lms.model.SellerPackage;
import swp391.fa25.lms.model.SellerSubscription;
import swp391.fa25.lms.repository.AccountRepository;
import swp391.fa25.lms.repository.PaymentTransactionRepository;
import swp391.fa25.lms.repository.SellerPackageRepository;
import swp391.fa25.lms.repository.SellerSubscriptionRepository;
import swp391.fa25.lms.util.VNPayUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/seller")
public class SellerPackageController {

    private final SellerPackageRepository packageRepo;
    private final SellerSubscriptionRepository subscriptionRepo;
    private final PaymentTransactionRepository transactionRepo;
    private final AccountRepository accountRepo;
    private final VNPayUtil vnPayUtil;

    @Value("${vnpay.returnUrlSeller}")
    private String returnUrlSeller;

    @Value("${vnpay.returnUrlSellerRegistration}")
    private String returnUrlSellerRegistration;

    @GetMapping("/register")
    public String showRegisterPage(HttpSession session, Model model) {
        Account user = (Account) session.getAttribute("loggedInAccount");
        if (user == null) return "redirect:/login";

        // Kiểm tra nếu đã là seller rồi
        if (user.getRole() != null && "SELLER".equals(user.getRole().getRoleName().toString())) {
            model.addAttribute("error", "Bạn đã là seller rồi!");
            return "redirect:/seller/renew";
        }

        List<SellerPackage> packages = packageRepo.findByStatus(SellerPackage.Status.ACTIVE);
        model.addAttribute("packages", packages);
        model.addAttribute("account", user);

        return "sellerpackage/seller-registration";
    }

    @GetMapping("/renew")
    public String showRenewPage(HttpSession session, Model model) {

        Account seller = (Account) session.getAttribute("loggedInAccount");
        if (seller == null) return "redirect:/login";

        List<SellerSubscription> subs = subscriptionRepo.findByAccountOrderByStartDateDesc(seller);
        boolean isNewSeller = subs.isEmpty();
        boolean expired = false;
        LocalDateTime expiryDate = null;

        if (!isNewSeller) {
            SellerSubscription latest = subs.get(0);
            expiryDate = latest.getEndDate();
            expired = expiryDate.isBefore(LocalDateTime.now());
        }
        model.addAttribute("isNewSeller", isNewSeller);
        model.addAttribute("expired", expired);
        model.addAttribute("currentExpiry", expiryDate);

        List<SellerPackage> packages = packageRepo.findByStatus(SellerPackage.Status.ACTIVE);
        model.addAttribute("packages", packages);

        return "sellerpackage/renew-package";
    }

    @PostMapping("/register")
    @Transactional
    public void performRegister(@RequestParam("packageId") int packageId,
                               HttpSession session,
                               HttpServletRequest request,
                               HttpServletResponse response) throws IOException {

        Account user = (Account) session.getAttribute("loggedInAccount");
        if (user == null) {
            response.sendRedirect("/login");
            return;
        }

        // Kiểm tra nếu đã là seller
        if (user.getRole() != null && "SELLER".equals(user.getRole().getRoleName().toString())) {
            response.sendRedirect("/seller/renew?error=already_seller");
            return;
        }

        SellerPackage pkg = packageRepo.findById(packageId).orElse(null);
        if (pkg == null) {
            response.sendRedirect("/seller/register?error=package_not_found");
            return;
        }

        String txnRef = vnPayUtil.generateTxnRef();

        // Tạo PaymentTransaction với type SELLER_REGISTRATION
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setAccount(user);
        transaction.setTransactionType(PaymentTransaction.TransactionType.SELLER_REGISTRATION);
        transaction.setStatus(PaymentTransaction.TransactionStatus.PENDING);
        transaction.setAmount(BigDecimal.valueOf(pkg.getPrice()));
        // OrderInfo format: REGISTER_{packageId}_{accountId}
        transaction.setDescription("REGISTER_" + packageId + "_" + user.getAccountId());
        transaction.setVnpayTxnRef(txnRef);
        transaction.setIpAddress(vnPayUtil.getIpAddress(request));
        transaction.setUserAgent(request.getHeader("User-Agent"));

        transaction = transactionRepo.save(transaction);

        String orderInfo = "REGISTER_" + packageId + "_" + user.getAccountId();
        String paymentUrl = vnPayUtil.createPaymentUrl((long) pkg.getPrice(), orderInfo, txnRef, returnUrlSellerRegistration, request);

        if (paymentUrl == null) {
            response.sendRedirect("/seller/register?error=create_url_failed");
            return;
        }

        transaction.setStatus(PaymentTransaction.TransactionStatus.PROCESSING);
        transactionRepo.save(transaction);

        response.sendRedirect(paymentUrl);
    }

    @PostMapping("/renew")
    @Transactional
    public void performRenew(@RequestParam("packageId") int packageId,
                             HttpSession session,
                             HttpServletRequest request,
                             HttpServletResponse response) throws IOException {

        Account seller = (Account) session.getAttribute("loggedInAccount");
        if (seller == null) {
            response.sendRedirect("/login");
            return;
        }

        SellerPackage pkg = packageRepo.findById(packageId).orElse(null);
        if (pkg == null) {
            response.sendRedirect("/seller/renew?error=package_not_found");
            return;
        }

        String txnRef = vnPayUtil.generateTxnRef();

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setAccount(seller);
        transaction.setTransactionType(PaymentTransaction.TransactionType.SELLER_SUBSCRIPTION);
        transaction.setStatus(PaymentTransaction.TransactionStatus.PENDING);
        transaction.setAmount(BigDecimal.valueOf(pkg.getPrice()));
        transaction.setDescription("SELLER_" + packageId + "_" + seller.getAccountId());
        transaction.setVnpayTxnRef(txnRef);
        transaction.setIpAddress(vnPayUtil.getIpAddress(request));
        transaction.setUserAgent(request.getHeader("User-Agent"));

        transaction = transactionRepo.save(transaction);

        String orderInfo = "SELLER_" + packageId + "_" + seller.getAccountId();
        String paymentUrl = vnPayUtil.createPaymentUrl((long) pkg.getPrice(), orderInfo, txnRef, returnUrlSeller, request);

        if (paymentUrl == null) {
            response.sendRedirect("/seller/renew?error=create_url_failed");
            return;
        }

        transaction.setStatus(PaymentTransaction.TransactionStatus.PROCESSING);
        transactionRepo.save(transaction);

        response.sendRedirect(paymentUrl);
    }
}
