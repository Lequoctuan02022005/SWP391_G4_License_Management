package swp391.fa25.lms.controller.payment;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import swp391.fa25.lms.model.*;
import swp391.fa25.lms.repository.*;
import swp391.fa25.lms.service.CartService;
import swp391.fa25.lms.service.OrderService;
import swp391.fa25.lms.util.VNPayUtil;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@RequestMapping("/payment")
public class PaymentController {

    private final PaymentTransactionRepository transactionRepo;
    private final SellerPackageRepository packageRepo;
    private final SellerSubscriptionRepository subscriptionRepo;
    private final AccountRepository accountRepo;
    private final RoleRepository roleRepo;
    private final OrderService orderService;
    private final CartService cartService;
    private final VNPayUtil vnPayUtil;

    @GetMapping("/seller-return")
    @Transactional
    public String handleSellerPaymentReturn(@RequestParam Map<String, String> params,
                                            HttpSession session,
                                            RedirectAttributes redirectAttrs) {
        try {
            boolean validHash = vnPayUtil.verifyHash(params);
            if (!validHash) {
                redirectAttrs.addFlashAttribute("error", "Chữ ký không hợp lệ! Vui lòng liên hệ Admin.");
                return "redirect:/seller/renew";
            }

            String vnp_ResponseCode = params.get("vnp_ResponseCode");
            String vnp_TxnRef = params.get("vnp_TxnRef");
            String vnp_OrderInfo = params.get("vnp_OrderInfo");

            Optional<PaymentTransaction> txOpt = transactionRepo.findByVnpayTxnRef(vnp_TxnRef);
            if (txOpt.isEmpty()) {
                redirectAttrs.addFlashAttribute("error", "Không tìm thấy giao dịch!");
                return "redirect:/seller/renew";
            }

            PaymentTransaction transaction = txOpt.get();
            transaction.setVnpayResponseCode(vnp_ResponseCode);

            if ("00".equals(vnp_ResponseCode)) {
                if (vnp_OrderInfo == null || !vnp_OrderInfo.startsWith("SELLER_")) {
                    redirectAttrs.addFlashAttribute("error", "Thông tin đơn hàng không hợp lệ!");
                    transaction.setStatus(PaymentTransaction.TransactionStatus.FAILED);
                    transactionRepo.save(transaction);
                    return "redirect:/seller/renew";
                }

                String[] parts = vnp_OrderInfo.split("_");
                if (parts.length != 3) {
                    redirectAttrs.addFlashAttribute("error", "Thông tin đơn hàng không đúng định dạng!");
                    transaction.setStatus(PaymentTransaction.TransactionStatus.FAILED);
                    transactionRepo.save(transaction);
                    return "redirect:/seller/renew";
                }

                int packageId = Integer.parseInt(parts[1]);
                Long accountId = Long.parseLong(parts[2]);

                SellerPackage pkg = packageRepo.findById(packageId).orElse(null);
                if (pkg == null) {
                    redirectAttrs.addFlashAttribute("error", "Gói không tồn tại!");
                    transaction.setStatus(PaymentTransaction.TransactionStatus.FAILED);
                    transactionRepo.save(transaction);
                    return "redirect:/seller/renew";
                }

                Account seller = accountRepo.findById(accountId).orElse(null);
                if (seller == null) {
                    redirectAttrs.addFlashAttribute("error", "Tài khoản không tồn tại!");
                    transaction.setStatus(PaymentTransaction.TransactionStatus.FAILED);
                    transactionRepo.save(transaction);
                    return "redirect:/seller/renew";
                }

                transaction.setStatus(PaymentTransaction.TransactionStatus.SUCCESS);
                transaction.setCompletedAt(LocalDateTime.now());
                transactionRepo.save(transaction);

                SellerSubscription newSub = new SellerSubscription();
                newSub.setAccount(seller);
                newSub.setSellerPackage(pkg);
                newSub.setStartDate(LocalDateTime.now());
                newSub.setEndDate(LocalDateTime.now().plusMonths(pkg.getDurationInMonths()));
                newSub.setPriceAtPurchase(pkg.getPrice());
                newSub.setActive(true);
                newSub.setTransaction(transaction);

                subscriptionRepo.save(newSub);

                seller.setSellerActive(true);
                seller.setSellerExpiryDate(newSub.getEndDate());
                accountRepo.save(seller);

                session.setAttribute("loggedInAccount", seller);

                redirectAttrs.addFlashAttribute("success",
                        "Thanh toán thành công! Gói Seller đã được kích hoạt. Bạn có thể bắt đầu bán tool ngay bây giờ!");
                return "redirect:/home";

            } else {
                transaction.setStatus(PaymentTransaction.TransactionStatus.FAILED);
                transaction.setCompletedAt(LocalDateTime.now());
                transactionRepo.save(transaction);

                String errorMsg = transaction.getVnpayResponseMessage();
                redirectAttrs.addFlashAttribute("error", "Thanh toán thất bại: " + errorMsg);
                return "redirect:/seller/renew";
            }
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttrs.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/seller/renew";
        }
    }

    @GetMapping("/seller-registration-return")
    @Transactional
    public String handleSellerRegistrationReturn(@RequestParam Map<String, String> params,
                                                 HttpSession session,
                                                 RedirectAttributes redirectAttrs) {
        try {
            boolean validHash = vnPayUtil.verifyHash(params);
            if (!validHash) {
                redirectAttrs.addFlashAttribute("error", "Chữ ký không hợp lệ! Vui lòng liên hệ Admin.");
                return "redirect:/seller/register";
            }

            String vnp_ResponseCode = params.get("vnp_ResponseCode");
            String vnp_TxnRef = params.get("vnp_TxnRef");
            String vnp_OrderInfo = params.get("vnp_OrderInfo");

            Optional<PaymentTransaction> txOpt = transactionRepo.findByVnpayTxnRef(vnp_TxnRef);
            if (txOpt.isEmpty()) {
                redirectAttrs.addFlashAttribute("error", "Không tìm thấy giao dịch!");
                return "redirect:/seller/register";
            }

            PaymentTransaction transaction = txOpt.get();
            transaction.setVnpayResponseCode(vnp_ResponseCode);

            if ("00".equals(vnp_ResponseCode)) {
                if (vnp_OrderInfo == null || !vnp_OrderInfo.startsWith("REGISTER_")) {
                    redirectAttrs.addFlashAttribute("error", "Thông tin đơn hàng không hợp lệ!");
                    transaction.setStatus(PaymentTransaction.TransactionStatus.FAILED);
                    transactionRepo.save(transaction);
                    return "redirect:/seller/register";
                }

                String[] parts = vnp_OrderInfo.split("_");
                if (parts.length != 3) {
                    redirectAttrs.addFlashAttribute("error", "Thông tin đơn hàng không đúng định dạng!");
                    transaction.setStatus(PaymentTransaction.TransactionStatus.FAILED);
                    transactionRepo.save(transaction);
                    return "redirect:/seller/register";
                }

                int packageId = Integer.parseInt(parts[1]);
                Long accountId = Long.parseLong(parts[2]);

                SellerPackage pkg = packageRepo.findById(packageId).orElse(null);
                if (pkg == null) {
                    redirectAttrs.addFlashAttribute("error", "Gói không tồn tại!");
                    transaction.setStatus(PaymentTransaction.TransactionStatus.FAILED);
                    transactionRepo.save(transaction);
                    return "redirect:/seller/register";
                }

                Account user = accountRepo.findById(accountId).orElse(null);
                if (user == null) {
                    redirectAttrs.addFlashAttribute("error", "Tài khoản không tồn tại!");
                    transaction.setStatus(PaymentTransaction.TransactionStatus.FAILED);
                    transactionRepo.save(transaction);
                    return "redirect:/seller/register";
                }

                // Chuyển role thành SELLER và SAVE NGAY
                Role sellerRole = roleRepo.findByRoleName(Role.RoleName.SELLER)
                        .orElseThrow(() -> new RuntimeException("Role SELLER không tồn tại!"));

                user.setRole(sellerRole);
                user.setSellerActive(true);
                user.setSellerPackage(pkg);

                // SAVE ACCOUNT NGAY ĐỂ COMMIT ROLE VÀO DB
                accountRepo.saveAndFlush(user);

                // Cập nhật transaction status
                transaction.setStatus(PaymentTransaction.TransactionStatus.SUCCESS);
                transaction.setCompletedAt(LocalDateTime.now());
                transactionRepo.save(transaction);

                // Tạo SellerSubscription
                SellerSubscription newSub = new SellerSubscription();
                newSub.setAccount(user);
                newSub.setSellerPackage(pkg);
                newSub.setStartDate(LocalDateTime.now());
                newSub.setEndDate(LocalDateTime.now().plusMonths(pkg.getDurationInMonths()));
                newSub.setPriceAtPurchase(pkg.getPrice());
                newSub.setActive(true);
                newSub.setTransaction(transaction);
                subscriptionRepo.save(newSub);

                // Cập nhật seller expiry date
                user.setSellerExpiryDate(newSub.getEndDate());
                accountRepo.saveAndFlush(user);

                // Cập nhật session
                session.setAttribute("loggedInAccount", user);

                redirectAttrs.addFlashAttribute("success",
                        "Chúc mừng! Bạn đã đăng ký thành công làm Seller. Bạn có thể bắt đầu bán tool ngay bây giờ!");
                return "redirect:/home";

            } else {
                // Thanh toán thất bại
                transaction.setStatus(PaymentTransaction.TransactionStatus.FAILED);
                transaction.setCompletedAt(LocalDateTime.now());
                transactionRepo.save(transaction);

                String errorMsg = transaction.getVnpayResponseMessage();
                redirectAttrs.addFlashAttribute("error", "Thanh toán thất bại: " + errorMsg);
                return "redirect:/seller/register";
            }

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttrs.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/seller/register";
        }
    }

    @GetMapping("/checkout-return")
    @Transactional
    public String handleCheckoutPaymentReturn(@RequestParam Map<String, String> params, HttpSession session, RedirectAttributes redirectAttrs) {
        try {
            boolean validHash = vnPayUtil.verifyHash(params);
            if (!validHash) {
                redirectAttrs.addFlashAttribute("error", "Chữ ký không hợp lệ! Vui lòng liên hệ Admin.");
                return "redirect:/checkout";
            }

            String vnp_ResponseCode = params.get("vnp_ResponseCode");
            String vnp_TxnRef = params.get("vnp_TxnRef");
            String vnp_OrderInfo = params.get("vnp_OrderInfo");

            PaymentTransaction transaction = transactionRepo.findByVnpayTxnRef(vnp_TxnRef).orElse(null);

            if (transaction == null) {
                redirectAttrs.addFlashAttribute("error", "Không tìm thấy giao dịch!");
                return "redirect:/checkout";
            }

            transaction.setVnpayResponseCode(vnp_ResponseCode);

            if ("00".equals(vnp_ResponseCode)) {
                // Thanh toán thành công
                transaction.setStatus(PaymentTransaction.TransactionStatus.SUCCESS);
                transaction.setCompletedAt(LocalDateTime.now());
                transactionRepo.save(transaction);

                // Xử lý orders: update status, giảm stock, tạo license accounts
                orderService.processSuccessfulPayment(transaction);

                // Clear cart
                Account account = transaction.getAccount();
                cartService.clearCart(account);

                redirectAttrs.addFlashAttribute("success",
                        "Thanh toán thành công! Bạn có thể xem thông tin tài khoản trong mục 'Đơn hàng của tôi'.");
                return "redirect:/checkout/success";

            } else {
                // Thanh toán thất bại
                transaction.setStatus(PaymentTransaction.TransactionStatus.FAILED);
                transaction.setCompletedAt(LocalDateTime.now());
                transactionRepo.save(transaction);

                // Update orders thành FAILED
                orderService.processFailedPayment(transaction);

                String errorMsg = transaction.getVnpayResponseMessage();
                redirectAttrs.addFlashAttribute("error", "Thanh toán thất bại: " + errorMsg);
                return "redirect:/checkout";
            }

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttrs.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/checkout";
        }
    }
}