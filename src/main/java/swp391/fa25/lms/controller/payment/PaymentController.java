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
    private final LicenseAccountRepository licenseAccountRepo;
    private final LicenseRepository licenseRepo;
    private final LicenseRenewLogRepository licenseRenewLogRepo;

    private void clearCheckoutSession(HttpSession session) {
        if (session == null) return;
        session.removeAttribute("CHECKOUT_MODE");
        session.removeAttribute("BUY_NOW_TOOL_ID");
        session.removeAttribute("BUY_NOW_LICENSE_ID");
        session.removeAttribute("BUY_NOW_QTY");
        session.removeAttribute("CHECKOUT_SELECTED_IDS");
    }

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
                return "redirect:/dashboard";

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

                Role sellerRole = roleRepo.findByRoleName(Role.RoleName.SELLER)
                        .orElseThrow(() -> new RuntimeException("Role SELLER không tồn tại!"));

                user.setRole(sellerRole);
                user.setSellerActive(true);
                user.setSellerPackage(pkg);

                accountRepo.saveAndFlush(user);

                transaction.setStatus(PaymentTransaction.TransactionStatus.SUCCESS);
                transaction.setCompletedAt(LocalDateTime.now());
                transactionRepo.save(transaction);

                SellerSubscription newSub = new SellerSubscription();
                newSub.setAccount(user);
                newSub.setSellerPackage(pkg);
                newSub.setStartDate(LocalDateTime.now());
                newSub.setEndDate(LocalDateTime.now().plusMonths(pkg.getDurationInMonths()));
                newSub.setPriceAtPurchase(pkg.getPrice());
                newSub.setActive(true);
                newSub.setTransaction(transaction);
                subscriptionRepo.save(newSub);

                user.setSellerExpiryDate(newSub.getEndDate());
                accountRepo.saveAndFlush(user);

                session.setAttribute("loggedInAccount", user);

                redirectAttrs.addFlashAttribute("success",
                        "Chúc mừng! Bạn đã đăng ký thành công làm Seller. Bạn có thể bắt đầu bán tool ngay bây giờ!");
                return "redirect:/dashboard";

            } else {
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
    public String handleCheckoutPaymentReturn(@RequestParam Map<String, String> params,
                                              HttpSession session,
                                              RedirectAttributes redirectAttrs) {
        try {
            boolean validHash = vnPayUtil.verifyHash(params);
            if (!validHash) {
                // ✅ dọn session luôn cho chắc
                clearCheckoutSession(session);

                redirectAttrs.addFlashAttribute("error", "Chữ ký không hợp lệ! Vui lòng liên hệ Admin.");
                return "redirect:/checkout";
            }

            String vnp_ResponseCode = params.get("vnp_ResponseCode");
            String vnp_TxnRef = params.get("vnp_TxnRef");

            PaymentTransaction transaction = transactionRepo.findByVnpayTxnRef(vnp_TxnRef).orElse(null);
            if (transaction == null) {
                clearCheckoutSession(session);

                redirectAttrs.addFlashAttribute("error", "Không tìm thấy giao dịch!");
                return "redirect:/checkout";
            }

            transaction.setVnpayResponseCode(vnp_ResponseCode);

            if ("00".equals(vnp_ResponseCode)) {
                // SUCCESS
                transaction.setStatus(PaymentTransaction.TransactionStatus.SUCCESS);
                transaction.setCompletedAt(LocalDateTime.now());
                transactionRepo.save(transaction);

                // xử lý orders: update status, giảm stock, tạo license accounts
                orderService.processSuccessfulPayment(transaction);

                Account account = transaction.getAccount();
                String desc = transaction.getDescription();

                if (desc != null && desc.startsWith("BUYNOW_")) {
                    // BUY NOW: không đụng giỏ hàng
                } else if (desc != null && desc.startsWith("CHECKOUT_SELECTED:")) {
                    // CHECKOUT_SELECTED:<cartId>:<id1,id2,...>
                    try {
                        String[] parts = desc.split(":");
                        if (parts.length >= 3) {
                            String idsCsv = parts[2];
                            for (String s : idsCsv.split(",")) {
                                try {
                                    Long cartItemId = Long.parseLong(s.trim());
                                    cartService.removeItem(account, cartItemId);
                                } catch (Exception ignored) {}
                            }
                        } else {
                            cartService.clearCart(account);
                        }
                    } catch (Exception e) {
                        cartService.clearCart(account);
                    }
                } else {
                    // checkout all
                    cartService.clearCart(account);
                }

                // ✅ QUAN TRỌNG: clear session để không bị dính mua ngay / selected
                clearCheckoutSession(session);

                redirectAttrs.addFlashAttribute("success",
                        "Thanh toán thành công! Bạn có thể xem thông tin tài khoản trong mục 'Đơn hàng của tôi'.");
                return "redirect:/checkout/success";

            } else {
                // FAILED
                transaction.setStatus(PaymentTransaction.TransactionStatus.FAILED);
                transaction.setCompletedAt(LocalDateTime.now());
                transactionRepo.save(transaction);

                orderService.processFailedPayment(transaction);

                // ✅ clear session để lần sau chọn giỏ hàng không bị dính
                clearCheckoutSession(session);

                String errorMsg = transaction.getVnpayResponseMessage();
                redirectAttrs.addFlashAttribute("error", "Thanh toán thất bại: " + errorMsg);
                return "redirect:/checkout";
            }

        } catch (Exception e) {
            e.printStackTrace();

            // ✅ clear session cả khi exception
            clearCheckoutSession(session);

            redirectAttrs.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/checkout";
        }
    }

    @GetMapping("/license-renew-return")
    @Transactional
    public String handleLicenseRenewReturn(@RequestParam Map<String, String> params,
                                           HttpSession session,
                                           RedirectAttributes ra) {
        try {
            boolean validHash = vnPayUtil.verifyHash(params);
            if (!validHash) {
                ra.addFlashAttribute("error", "Chữ ký không hợp lệ!");
                return "redirect:/customer/license-accounts";
            }

            String vnp_ResponseCode = params.get("vnp_ResponseCode");
            String vnp_TxnRef = params.get("vnp_TxnRef");
            String vnp_OrderInfo = params.get("vnp_OrderInfo");

            Optional<PaymentTransaction> txOpt = transactionRepo.findByVnpayTxnRef(vnp_TxnRef);
            if (txOpt.isEmpty()) {
                ra.addFlashAttribute("error", "Không tìm thấy giao dịch!");
                return "redirect:/customer/license-accounts";
            }

            PaymentTransaction transaction = txOpt.get();
            transaction.setVnpayResponseCode(vnp_ResponseCode);
            transaction.setVnpayTransactionNo(params.get("vnp_TransactionNo"));
            transaction.setVnpayBankCode(params.get("vnp_BankCode"));
            transaction.setVnpayBankTranNo(params.get("vnp_BankTranNo"));
            transaction.setVnpayCardType(params.get("vnp_CardType"));
            transaction.setVnpayPayDate(params.get("vnp_PayDate"));

            if ("00".equals(vnp_ResponseCode)) {
                if (vnp_OrderInfo == null || !vnp_OrderInfo.startsWith("RENEW_LICENSE_")) {
                    ra.addFlashAttribute("error", "Thông tin đơn hàng không hợp lệ!");
                    transaction.setStatus(PaymentTransaction.TransactionStatus.FAILED);
                    transactionRepo.save(transaction);
                    return "redirect:/customer/license-accounts";
                }

                String[] parts = vnp_OrderInfo.split("_");
                if (parts.length != 5) {
                    ra.addFlashAttribute("error", "Thông tin đơn hàng không đúng định dạng!");
                    transaction.setStatus(PaymentTransaction.TransactionStatus.FAILED);
                    transactionRepo.save(transaction);
                    return "redirect:/customer/license-accounts";
                }

                Long licenseAccountId = Long.parseLong(parts[2]);
                Long licenseId = Long.parseLong(parts[3]);

                LicenseAccount licenseAccount = licenseAccountRepo.findById(licenseAccountId)
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy License Account!"));

                License license = licenseRepo.findById(licenseId)
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gói gia hạn!"));

                LocalDateTime oldEndDate = licenseAccount.getEndDate();
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime baseDate = (oldEndDate != null && oldEndDate.isAfter(now)) ? oldEndDate : now;
                LocalDateTime newEndDate = baseDate.plusDays(license.getDurationDays());

                licenseAccount.setEndDate(newEndDate);
                licenseAccount.setStatus(LicenseAccount.Status.ACTIVE);
                licenseAccountRepo.save(licenseAccount);

                LicenseRenewLog renewLog = new LicenseRenewLog();
                renewLog.setLicenseAccount(licenseAccount);
                renewLog.setRenewDate(LocalDateTime.now());
                renewLog.setNewEndDate(newEndDate);
                renewLog.setAmountPaid(transaction.getAmount());
                renewLog.setTransaction(transaction);
                licenseRenewLogRepo.save(renewLog);

                transaction.setStatus(PaymentTransaction.TransactionStatus.SUCCESS);
                transaction.setCompletedAt(LocalDateTime.now());
                transactionRepo.save(transaction);

                ra.addFlashAttribute("success", "Gia hạn license thành công! Hạn mới: " +
                        newEndDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                return "redirect:/customer/license-accounts/" + licenseAccountId;

            } else {
                transaction.setStatus(PaymentTransaction.TransactionStatus.FAILED);
                transactionRepo.save(transaction);

                String errorMsg = transaction.getVnpayResponseMessage();
                ra.addFlashAttribute("error", "Gia hạn thất bại: " + errorMsg);
                return "redirect:/customer/license-accounts";
            }

        } catch (Exception e) {
            e.printStackTrace();
            ra.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/customer/license-accounts";
        }
    }
}
