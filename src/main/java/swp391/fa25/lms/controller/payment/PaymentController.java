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
import java.time.format.DateTimeFormatter;
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

    private final LicenseAccountRepository licenseAccountRepo;
    private final LicenseRepository licenseRepo;
    private final LicenseRenewLogRepository licenseRenewLogRepo;

    // ===================== helpers =====================

    private void clearCheckoutSession(HttpSession session) {
        if (session == null) return;
        session.removeAttribute("CHECKOUT_MODE");
        session.removeAttribute("BUY_NOW_TOOL_ID");
        session.removeAttribute("BUY_NOW_LICENSE_ID");
        session.removeAttribute("BUY_NOW_QTY");
        session.removeAttribute("CHECKOUT_SELECTED_IDS");
    }

    private boolean isVnpSuccess(String code) {
        return "00".equals(code);
    }

    /** set cả 2 key để view nào đọc success/successMsg đều chạy */
    private void flashSuccess(RedirectAttributes ra, String msg) {
        ra.addFlashAttribute("success", msg);
        ra.addFlashAttribute("successMsg", msg);
    }

    private void flashError(RedirectAttributes ra, String msg) {
        ra.addFlashAttribute("error", msg);
        ra.addFlashAttribute("errorMsg", msg);
    }

    private void fillVnpFields(PaymentTransaction tx, Map<String, String> params) {
        if (tx == null || params == null) return;

        tx.setVnpayResponseCode(params.get("vnp_ResponseCode"));

        // các field này anh đã dùng ở renew => assume entity có setter
        tx.setVnpayTransactionNo(params.get("vnp_TransactionNo"));
        tx.setVnpayBankCode(params.get("vnp_BankCode"));
        tx.setVnpayBankTranNo(params.get("vnp_BankTranNo"));
        tx.setVnpayCardType(params.get("vnp_CardType"));
        tx.setVnpayPayDate(params.get("vnp_PayDate"));
    }

    private String safeVnpMessage(PaymentTransaction tx) {
        try {
            String m = tx.getVnpayResponseMessage();
            return (m == null || m.isBlank()) ? "Vui lòng thử lại hoặc liên hệ Admin." : m;
        } catch (Exception e) {
            return "Vui lòng thử lại hoặc liên hệ Admin.";
        }
    }

    private boolean isFinalStatus(PaymentTransaction tx) {
        if (tx == null || tx.getStatus() == null) return false;
        return tx.getStatus() == PaymentTransaction.TransactionStatus.SUCCESS
                || tx.getStatus() == PaymentTransaction.TransactionStatus.FAILED;
    }

    // ===================== SELLER RENEW RETURN =====================

    @GetMapping("/seller-return")
    @Transactional
    public String handleSellerPaymentReturn(@RequestParam Map<String, String> params,
                                            HttpSession session,
                                            RedirectAttributes ra) {
        try {
            if (!vnPayUtil.verifyHash(params)) {
                flashError(ra, "Chữ ký không hợp lệ! Vui lòng liên hệ Admin.");
                return "redirect:/seller/renew";
            }

            String vnpCode = params.get("vnp_ResponseCode");
            String vnpTxnRef = params.get("vnp_TxnRef");
            String vnpOrderInfo = params.get("vnp_OrderInfo");

            PaymentTransaction tx = transactionRepo.findByVnpayTxnRef(vnpTxnRef).orElse(null);
            if (tx == null) {
                flashError(ra, "Không tìm thấy giao dịch!");
                return "redirect:/seller/renew";
            }

            // idempotent: refresh return không xử lý lại
            if (isFinalStatus(tx)) {
                if (tx.getStatus() == PaymentTransaction.TransactionStatus.SUCCESS) {
                    flashSuccess(ra, "Giao dịch đã xử lý thành công trước đó.");
                    return "redirect:/dashboard";
                }
                flashError(ra, "Giao dịch đã xử lý thất bại trước đó.");
                return "redirect:/seller/renew";
            }

            fillVnpFields(tx, params);

            if (!isVnpSuccess(vnpCode)) {
                tx.setStatus(PaymentTransaction.TransactionStatus.FAILED);
                tx.setCompletedAt(LocalDateTime.now());
                transactionRepo.save(tx);

                flashError(ra, "Thanh toán thất bại: " + safeVnpMessage(tx));
                return "redirect:/seller/renew";
            }

            // success: parse orderInfo: SELLER_<packageId>_<accountId>
            if (vnpOrderInfo == null || !vnpOrderInfo.startsWith("SELLER_")) {
                tx.setStatus(PaymentTransaction.TransactionStatus.FAILED);
                tx.setCompletedAt(LocalDateTime.now());
                transactionRepo.save(tx);

                flashError(ra, "Thông tin đơn hàng không hợp lệ!");
                return "redirect:/seller/renew";
            }

            String[] parts = vnpOrderInfo.split("_");
            if (parts.length != 3) {
                tx.setStatus(PaymentTransaction.TransactionStatus.FAILED);
                tx.setCompletedAt(LocalDateTime.now());
                transactionRepo.save(tx);

                flashError(ra, "Thông tin đơn hàng không đúng định dạng!");
                return "redirect:/seller/renew";
            }

            int packageId = Integer.parseInt(parts[1]);
            Long accountId = Long.parseLong(parts[2]);

            SellerPackage pkg = packageRepo.findById(packageId).orElse(null);
            Account seller = accountRepo.findById(accountId).orElse(null);

            if (pkg == null || seller == null) {
                tx.setStatus(PaymentTransaction.TransactionStatus.FAILED);
                tx.setCompletedAt(LocalDateTime.now());
                transactionRepo.save(tx);

                flashError(ra, pkg == null ? "Gói không tồn tại!" : "Tài khoản không tồn tại!");
                return "redirect:/seller/renew";
            }

            // mark tx success
            tx.setStatus(PaymentTransaction.TransactionStatus.SUCCESS);
            tx.setCompletedAt(LocalDateTime.now());
            transactionRepo.save(tx);
            LocalDateTime now = LocalDateTime.now();

            SellerSubscription last =
                    subscriptionRepo.findTopByAccountOrderByEndDateDesc(seller)
                            .orElse(null);

            LocalDateTime base =
                    (last != null && last.getEndDate().isAfter(now))
                            ? last.getEndDate()
                            : now;

// TẠO RECORD MỚI (KHÔNG ĐỤNG RECORD CŨ)
            SellerSubscription newSub = new SellerSubscription();
            newSub.setAccount(seller);
            newSub.setSellerPackage(pkg);
            newSub.setStartDate(base);
            newSub.setEndDate(base.plusMonths(pkg.getDurationInMonths()));
            newSub.setPriceAtPurchase(pkg.getPrice());
            newSub.setActive(true);
            newSub.setTransaction(tx);

            subscriptionRepo.save(newSub);

            // Update seller active status and expiry date
            seller.setSellerActive(true);
            seller.setSellerExpiryDate(newSub.getEndDate());
            accountRepo.saveAndFlush(seller);

            session.setAttribute("loggedInAccount", seller);

            flashSuccess(ra, "Thanh toán thành công! Gói Seller đã được kích hoạt. Bạn có thể bắt đầu bán tool ngay bây giờ!");
            return "redirect:/dashboard";

        } catch (Exception e) {
            e.printStackTrace();
            flashError(ra, "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/seller/renew";
        }
    }

    // ===================== SELLER REGISTRATION RETURN =====================

    @GetMapping("/seller-registration-return")
    @Transactional
    public String handleSellerRegistrationReturn(@RequestParam Map<String, String> params,
                                                 HttpSession session,
                                                 RedirectAttributes ra) {
        try {
            if (!vnPayUtil.verifyHash(params)) {
                flashError(ra, "Chữ ký không hợp lệ! Vui lòng liên hệ Admin.");
                return "redirect:/seller/register";
            }

            String vnpCode = params.get("vnp_ResponseCode");
            String vnpTxnRef = params.get("vnp_TxnRef");
            String vnpOrderInfo = params.get("vnp_OrderInfo");

            PaymentTransaction tx = transactionRepo.findByVnpayTxnRef(vnpTxnRef).orElse(null);
            if (tx == null) {
                flashError(ra, "Không tìm thấy giao dịch!");
                return "redirect:/seller/register";
            }

            // idempotent
            if (isFinalStatus(tx)) {
                if (tx.getStatus() == PaymentTransaction.TransactionStatus.SUCCESS) {
                    flashSuccess(ra, "Giao dịch đã xử lý thành công trước đó.");
                    return "redirect:/dashboard";
                }
                flashError(ra, "Giao dịch đã xử lý thất bại trước đó.");
                return "redirect:/seller/register";
            }

            fillVnpFields(tx, params);

            if (!isVnpSuccess(vnpCode)) {
                tx.setStatus(PaymentTransaction.TransactionStatus.FAILED);
                tx.setCompletedAt(LocalDateTime.now());
                transactionRepo.save(tx);

                flashError(ra, "Thanh toán thất bại: " + safeVnpMessage(tx));
                return "redirect:/seller/register";
            }

            // REGISTER_<packageId>_<accountId>
            if (vnpOrderInfo == null || !vnpOrderInfo.startsWith("REGISTER_")) {
                tx.setStatus(PaymentTransaction.TransactionStatus.FAILED);
                tx.setCompletedAt(LocalDateTime.now());
                transactionRepo.save(tx);

                flashError(ra, "Thông tin đơn hàng không hợp lệ!");
                return "redirect:/seller/register";
            }

            String[] parts = vnpOrderInfo.split("_");
            if (parts.length != 3) {
                tx.setStatus(PaymentTransaction.TransactionStatus.FAILED);
                tx.setCompletedAt(LocalDateTime.now());
                transactionRepo.save(tx);

                flashError(ra, "Thông tin đơn hàng không đúng định dạng!");
                return "redirect:/seller/register";
            }

            int packageId = Integer.parseInt(parts[1]);
            Long accountId = Long.parseLong(parts[2]);

            SellerPackage pkg = packageRepo.findById(packageId).orElse(null);
            Account user = accountRepo.findById(accountId).orElse(null);

            if (pkg == null || user == null) {
                tx.setStatus(PaymentTransaction.TransactionStatus.FAILED);
                tx.setCompletedAt(LocalDateTime.now());
                transactionRepo.save(tx);

                flashError(ra, pkg == null ? "Gói không tồn tại!" : "Tài khoản không tồn tại!");
                return "redirect:/seller/register";
            }

            Role sellerRole = roleRepo.findByRoleName(Role.RoleName.SELLER)
                    .orElseThrow(() -> new RuntimeException("Role SELLER không tồn tại!"));

            user.setRole(sellerRole);
            user.setSellerActive(true);
            user.setSellerPackage(pkg);
            accountRepo.saveAndFlush(user);

            // tx success
            tx.setStatus(PaymentTransaction.TransactionStatus.SUCCESS);
            tx.setCompletedAt(LocalDateTime.now());
            transactionRepo.save(tx);

            SellerSubscription newSub = new SellerSubscription();
            newSub.setAccount(user);
            newSub.setSellerPackage(pkg);
            newSub.setStartDate(LocalDateTime.now());
            newSub.setEndDate(LocalDateTime.now().plusMonths(pkg.getDurationInMonths()));
            newSub.setPriceAtPurchase(pkg.getPrice());
            newSub.setActive(true);
            newSub.setTransaction(tx);
            subscriptionRepo.save(newSub);

            user.setSellerExpiryDate(newSub.getEndDate());
            accountRepo.saveAndFlush(user);

            session.setAttribute("loggedInAccount", user);

            flashSuccess(ra, "Chúc mừng! Bạn đã đăng ký thành công làm Seller. Bạn có thể bắt đầu bán tool ngay bây giờ!");
            return "redirect:/dashboard";

        } catch (Exception e) {
            e.printStackTrace();
            flashError(ra, "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/seller/register";
        }
    }

    // ===================== CHECKOUT RETURN =====================

    @GetMapping("/checkout-return")
    @Transactional
    public String handleCheckoutPaymentReturn(@RequestParam Map<String, String> params,
                                              HttpSession session,
                                              RedirectAttributes ra) {
        try {
            if (!vnPayUtil.verifyHash(params)) {
                clearCheckoutSession(session);
                flashError(ra, "Chữ ký không hợp lệ! Vui lòng liên hệ Admin.");
                return "redirect:/checkout";
            }

            String vnpCode = params.get("vnp_ResponseCode");
            String vnpTxnRef = params.get("vnp_TxnRef");

            PaymentTransaction tx = transactionRepo.findByVnpayTxnRef(vnpTxnRef).orElse(null);
            if (tx == null) {
                clearCheckoutSession(session);
                flashError(ra, "Không tìm thấy giao dịch!");
                return "redirect:/checkout";
            }

            // idempotent
            if (isFinalStatus(tx)) {
                clearCheckoutSession(session);
                if (tx.getStatus() == PaymentTransaction.TransactionStatus.SUCCESS) {
                    return "redirect:/checkout/success";
                }
                return "redirect:/checkout";
            }

            fillVnpFields(tx, params);

            if (isVnpSuccess(vnpCode)) {
                tx.setStatus(PaymentTransaction.TransactionStatus.SUCCESS);
                tx.setCompletedAt(LocalDateTime.now());
                transactionRepo.save(tx);

                // update order + tạo license accounts...
                orderService.processSuccessfulPayment(tx);

                // dọn cart theo loại checkout
                Account account = tx.getAccount();
                String desc = tx.getDescription();

                if (desc != null && desc.startsWith("BUYNOW_")) {
                    // buy now: không đụng giỏ hàng
                } else if (desc != null && desc.startsWith("CHECKOUT_SELECTED:")) {
                    // format chuẩn: CHECKOUT_SELECTED:<id1,id2,...>
                    String idsCsv = desc.substring("CHECKOUT_SELECTED:".length()).trim();
                    if (!idsCsv.isBlank()) {
                        for (String s : idsCsv.split(",")) {
                            try {
                                Long cartItemId = Long.parseLong(s.trim());
                                cartService.removeItem(account, cartItemId);
                            } catch (Exception ignored) {}
                        }
                    } else {
                        cartService.clearCart(account);
                    }
                } else {
                    cartService.clearCart(account);
                }

                clearCheckoutSession(session);

                flashSuccess(ra, "Thanh toán thành công! Bạn có thể xem thông tin tài khoản trong mục 'Đơn hàng của tôi'.");
                return "redirect:/checkout/success";

            } else {
                tx.setStatus(PaymentTransaction.TransactionStatus.FAILED);
                tx.setCompletedAt(LocalDateTime.now());
                transactionRepo.save(tx);

                orderService.processFailedPayment(tx);

                clearCheckoutSession(session);

                flashError(ra, "Thanh toán thất bại: " + safeVnpMessage(tx));
                return "redirect:/checkout";
            }

        } catch (Exception e) {
            e.printStackTrace();
            clearCheckoutSession(session);
            flashError(ra, "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/checkout";
        }
    }

    // ===================== LICENSE RENEW RETURN =====================

    @GetMapping("/license-renew-return")
    @Transactional
    public String handleLicenseRenewReturn(@RequestParam Map<String, String> params,
                                           RedirectAttributes ra) {
        try {
            if (!vnPayUtil.verifyHash(params)) {
                flashError(ra, "Chữ ký không hợp lệ!");
                return "redirect:/customer/license-accounts";
            }

            String vnpCode = params.get("vnp_ResponseCode");
            String vnpTxnRef = params.get("vnp_TxnRef");
            String vnpOrderInfo = params.get("vnp_OrderInfo");

            PaymentTransaction tx = transactionRepo.findByVnpayTxnRef(vnpTxnRef).orElse(null);
            if (tx == null) {
                flashError(ra, "Không tìm thấy giao dịch!");
                return "redirect:/customer/license-accounts";
            }

            // idempotent
            if (isFinalStatus(tx)) {
                if (tx.getStatus() == PaymentTransaction.TransactionStatus.SUCCESS) {
                    flashSuccess(ra, "Giao dịch đã xử lý thành công trước đó.");
                } else {
                    flashError(ra, "Giao dịch đã xử lý thất bại trước đó.");
                }
                return "redirect:/customer/license-accounts";
            }

            fillVnpFields(tx, params);

            if (!isVnpSuccess(vnpCode)) {
                tx.setStatus(PaymentTransaction.TransactionStatus.FAILED);
                tx.setCompletedAt(LocalDateTime.now());
                transactionRepo.save(tx);

                flashError(ra, "Gia hạn thất bại: " + safeVnpMessage(tx));
                return "redirect:/customer/license-accounts";
            }

            // format linh hoạt:
            // RENEW_LICENSE_<licenseAccountId>_<licenseId> (thường là 4 phần)
            if (vnpOrderInfo == null || !vnpOrderInfo.startsWith("RENEW_LICENSE_")) {
                tx.setStatus(PaymentTransaction.TransactionStatus.FAILED);
                tx.setCompletedAt(LocalDateTime.now());
                transactionRepo.save(tx);

                flashError(ra, "Thông tin đơn hàng không hợp lệ!");
                return "redirect:/customer/license-accounts";
            }

            String[] parts = vnpOrderInfo.split("_");
            if (parts.length < 4) {
                tx.setStatus(PaymentTransaction.TransactionStatus.FAILED);
                tx.setCompletedAt(LocalDateTime.now());
                transactionRepo.save(tx);

                flashError(ra, "Thông tin đơn hàng không đúng định dạng!");
                return "redirect:/customer/license-accounts";
            }

            Long licenseAccountId = Long.parseLong(parts[2]);
            Long licenseId = Long.parseLong(parts[3]);

            LicenseAccount licenseAccount = licenseAccountRepo.findById(licenseAccountId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy License Account!"));

            License license = licenseRepo.findById(licenseId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gói gia hạn!"));

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime oldEnd = licenseAccount.getEndDate();
            LocalDateTime base = (oldEnd != null && oldEnd.isAfter(now)) ? oldEnd : now;
            LocalDateTime newEnd = base.plusDays(license.getDurationDays());

            licenseAccount.setEndDate(newEnd);
            licenseAccount.setStatus(LicenseAccount.Status.ACTIVE);
            licenseAccountRepo.save(licenseAccount);

            LicenseRenewLog renewLog = new LicenseRenewLog();
            renewLog.setLicenseAccount(licenseAccount);
            renewLog.setRenewDate(now);
            renewLog.setNewEndDate(newEnd);
            renewLog.setAmountPaid(tx.getAmount());
            renewLog.setTransaction(tx);
            licenseRenewLogRepo.save(renewLog);

            tx.setStatus(PaymentTransaction.TransactionStatus.SUCCESS);
            tx.setCompletedAt(now);
            transactionRepo.save(tx);

            String endText = newEnd.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            flashSuccess(ra, "Gia hạn license thành công! Hạn mới: " + endText);
            return "redirect:/customer/license-accounts/" + licenseAccountId;

        } catch (Exception e) {
            e.printStackTrace();
            flashError(ra, "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/customer/license-accounts";
        }
    }

    // ===================== REPAY RETURN =====================

    @GetMapping("/repay-return")
    public String handleRepayReturn(@RequestParam Map<String, String> params,
                                    RedirectAttributes ra) {
        try {
            if (!vnPayUtil.verifyHash(params)) {
                ra.addFlashAttribute("errorMsg", "Chữ ký không hợp lệ!");
                return "redirect:/customer/orders";
            }

            String code = params.get("vnp_ResponseCode");
            String ref  = params.get("vnp_TxnRef");

            PaymentTransaction tx = transactionRepo.findByVnpayTxnRef(ref).orElse(null);
            if (tx == null) {
                ra.addFlashAttribute("errorMsg", "Không tìm thấy giao dịch!");
                return "redirect:/customer/orders";
            }

            // fill vnp fields
            tx.setVnpayResponseCode(code);
            tx.setVnpayTransactionNo(params.get("vnp_TransactionNo"));
            tx.setVnpayBankCode(params.get("vnp_BankCode"));
            tx.setVnpayBankTranNo(params.get("vnp_BankTranNo"));
            tx.setVnpayCardType(params.get("vnp_CardType"));
            tx.setVnpayPayDate(params.get("vnp_PayDate"));

            Long orderId = extractOrderIdFromRepayDesc(tx.getDescription());

            // ✅ chốt trạng thái tx trước để không bị “treo”
            if ("00".equals(code)) {
                tx.setStatus(PaymentTransaction.TransactionStatus.SUCCESS);
                tx.setCompletedAt(LocalDateTime.now());
                transactionRepo.save(tx);

                try {
                    // nếu service có issue cũng không làm tx quay lại PROCESSING
                    orderService.processSuccessfulPayment(tx);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    ra.addFlashAttribute("errorMsg",
                            "Thanh toán VNPay thành công nhưng hệ thống xử lý đơn bị lỗi. Vui lòng báo Admin (txnRef=" + ref + ").");
                    return (orderId != null) ? "redirect:/customer/orders/" + orderId : "redirect:/customer/orders";
                }

                ra.addFlashAttribute("successMsg", "Thanh toán lại thành công!");
                return (orderId != null) ? "redirect:/customer/orders/" + orderId : "redirect:/customer/orders";

            } else {
                tx.setStatus(PaymentTransaction.TransactionStatus.FAILED);
                tx.setCompletedAt(LocalDateTime.now());
                transactionRepo.save(tx);

                try {
                    orderService.processFailedPayment(tx);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    // vẫn OK: tx đã FAILED rồi, không treo
                }

                ra.addFlashAttribute("errorMsg", "Thanh toán lại thất bại: " + tx.getVnpayResponseMessage());
                return (orderId != null) ? "redirect:/customer/orders/" + orderId : "redirect:/customer/orders";
            }

        } catch (Exception e) {
            e.printStackTrace();
            ra.addFlashAttribute("errorMsg", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/customer/orders";
        }
    }

    private Long extractOrderIdFromRepayDesc(String desc) {
        if (desc ==null)
            return null;
        String prefix ="BUYNOW_REPAY_ORDER_";
        if(!desc.startsWith(prefix))
            return null;
        try {
            return Long.parseLong(desc.substring(prefix.length()).trim());
        } catch (Exception e){
            return null;
        }
    }

}
