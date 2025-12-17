package swp391.fa25.lms.controller.checkout;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import swp391.fa25.lms.model.*;
import swp391.fa25.lms.repository.*;
import swp391.fa25.lms.service.CartService;
import swp391.fa25.lms.service.OrderService;
import swp391.fa25.lms.util.VNPayUtil;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/checkout")
public class CheckoutController {

    private final CartService cartService;
    private final OrderService orderService;
    private final PaymentTransactionRepository transactionRepo;
    private final VNPayUtil vnPayUtil;

    private final ToolRepository toolRepo;
    private final LicenseRepository licenseRepo;

    @Value("${vnpay.returnUrlCheckout}")
    private String returnUrlCheckout;

    /**
     * Checkout default (từ giỏ hàng)
     * Nếu session có BUY_NOW thì ưu tiên confirm buy-now
     */
    @GetMapping
    public String showCheckout(@RequestParam(value = "selected", required = false) String selected,
                               Model model,
                               HttpServletRequest request) {

        Account account = (Account) request.getSession().getAttribute("loggedInAccount");
        if (account == null) return "redirect:/login";

        request.getSession().setAttribute("CHECKOUT_MODE", "CART");
        request.getSession().removeAttribute("BUY_NOW_TOOL_ID");
        request.getSession().removeAttribute("BUY_NOW_LICENSE_ID");
        request.getSession().removeAttribute("BUY_NOW_QTY");

        Cart cart = cartService.getCartWithItems(account);
        var items = cart.getItems();

        if (items == null || items.isEmpty()) {
            return "redirect:/cart/view";
        }

        List<CartItem> payItems = items;

        if (selected != null && !selected.trim().isEmpty()) {
            Set<Long> idSet = new HashSet<>();
            for (String s : selected.split(",")) {
                try { idSet.add(Long.parseLong(s.trim())); } catch (Exception ignored) {}
            }

            payItems = items.stream()
                    .filter(ci -> ci.getCartItemId() != null && idSet.contains(ci.getCartItemId()))
                    .toList();

            if (payItems.isEmpty()) {
                return "redirect:/cart/view?error=selected_empty";
            }

            request.getSession().setAttribute("CHECKOUT_SELECTED_IDS", new ArrayList<>(idSet));
        } else {
            request.getSession().removeAttribute("CHECKOUT_SELECTED_IDS");
        }

        double total = payItems.stream().mapToDouble(CartItem::getTotalPrice).sum();

        model.addAttribute("account", account);
        model.addAttribute("items", payItems);
        model.addAttribute("total", total);

        return "checkout/confirm";
    }

    /**
     * ✅ BUY NOW: lưu toolId + licenseId + qty vào session
     */
    @PostMapping("/buy-now")
    @ResponseBody
    public Map<String, Object> buyNow(@RequestParam Long toolId,
                                      @RequestParam Long licenseId,
                                      @RequestParam(defaultValue = "1") Integer quantity,
                                      HttpServletRequest request) {
        Account account = (Account) request.getSession().getAttribute("loggedInAccount");
        if (account == null) {
            return Map.of("success", false, "message", "Bạn cần đăng nhập!");
        }
        if (quantity == null || quantity <= 0) quantity = 1;

        request.getSession().setAttribute("CHECKOUT_MODE", "BUY_NOW");
        request.getSession().setAttribute("BUY_NOW_TOOL_ID", toolId);
        request.getSession().setAttribute("BUY_NOW_LICENSE_ID", licenseId);
        request.getSession().setAttribute("BUY_NOW_QTY", quantity);

        return Map.of("success", true, "redirectUrl", "/checkout/buy-now/confirm");
    }

    /**
     * ✅ Confirm BUY NOW: render checkout/confirm nhưng items = sản phẩm đang xem
     * (Không tạo file view mới)
     */
    @GetMapping("/buy-now/confirm")
    public String showBuyNowConfirm(Model model, HttpServletRequest request) {
        Account account = (Account) request.getSession().getAttribute("loggedInAccount");
        if (account == null) return "redirect:/login";

        Long toolId = (Long) request.getSession().getAttribute("BUY_NOW_TOOL_ID");
        Long licenseId = (Long) request.getSession().getAttribute("BUY_NOW_LICENSE_ID");
        Integer qty = (Integer) request.getSession().getAttribute("BUY_NOW_QTY");

        if (toolId == null || licenseId == null) {
            return "redirect:/cart/view";
        }
        if (qty == null || qty <= 0) qty = 1;

        Tool tool = toolRepo.findById(toolId).orElse(null);
        License license = licenseRepo.findById(licenseId).orElse(null);

        if (tool == null || license == null) {
            return "redirect:/cart/view";
        }

        if (tool.getAvailableQuantity() != null && tool.getAvailableQuantity() <= 0) {
            return "redirect:/tool/" + toolId + "?error=out_of_stock";
        }

        Map<String, Object> item = new HashMap<>();
        item.put("tool", tool);
        item.put("license", license);

        double unitPrice = license.getPrice();
        double totalPrice = unitPrice * qty;

        item.put("unitPrice", unitPrice);
        item.put("quantity", qty);
        item.put("totalPrice", totalPrice);

        model.addAttribute("account", account);
        model.addAttribute("items", List.of(item));
        model.addAttribute("total", totalPrice);

        return "checkout/confirm";
    }

    /**
     * ✅ Payment:
     * - Nếu có BUY_NOW => thanh toán sản phẩm đang xem
     * - Không có => thanh toán cart (ALL hoặc SELECTED)
     */

    @PostMapping("/payment")
    @Transactional
    public void performPayment(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Account account = (Account) request.getSession().getAttribute("loggedInAccount");
        if (account == null) {
            response.sendRedirect("/login");
            return;
        }

        try {
            String mode = (String) request.getSession().getAttribute("CHECKOUT_MODE");
            boolean isBuyNow = "BUY_NOW".equalsIgnoreCase(mode);

            // ================== BUY_NOW ==================
            if (isBuyNow) {
                Long toolId = (Long) request.getSession().getAttribute("BUY_NOW_TOOL_ID");
                Long licenseId = (Long) request.getSession().getAttribute("BUY_NOW_LICENSE_ID");
                Integer qty = (Integer) request.getSession().getAttribute("BUY_NOW_QTY");
                if (qty == null || qty <= 0) qty = 1;

                if (toolId == null || licenseId == null) {
                    response.sendRedirect("/cart/view?error=buy_now_missing");
                    return;
                }

                Tool tool = toolRepo.findById(toolId).orElse(null);
                License license = licenseRepo.findById(licenseId).orElse(null);

                if (tool == null || license == null) {
                    response.sendRedirect("/cart/view?error=buy_now_invalid");
                    return;
                }

                if (tool.getAvailableQuantity() != null && tool.getAvailableQuantity() <= 0) {
                    response.sendRedirect("/tool/" + toolId + "?error=out_of_stock");
                    return;
                }

                double total = license.getPrice() * qty;

                String txnRef = vnPayUtil.generateTxnRef();
                PaymentTransaction transaction = new PaymentTransaction();
                transaction.setAccount(account);
                transaction.setTransactionType(PaymentTransaction.TransactionType.ORDER_PAYMENT);
                transaction.setStatus(PaymentTransaction.TransactionStatus.PENDING);
                transaction.setAmount(BigDecimal.valueOf(total));
                transaction.setDescription("BUYNOW_" + toolId + "_" + licenseId + "_Q" + qty);
                transaction.setVnpayTxnRef(txnRef);
                transaction.setIpAddress(vnPayUtil.getIpAddress(request));
                transaction.setUserAgent(request.getHeader("User-Agent"));
                transaction = transactionRepo.save(transaction);

                orderService.createOrderFromBuyNow(account, tool, license, qty, transaction);

                String orderInfo = "CHECKOUT_" + transaction.getTransactionId();
                String paymentUrl = vnPayUtil.createPaymentUrl(
                        (long) total,
                        orderInfo,
                        txnRef,
                        returnUrlCheckout,
                        request
                );

                if (paymentUrl == null) {
                    response.sendRedirect("/checkout/buy-now/confirm?error=create_url_failed");
                    return;
                }

                transaction.setStatus(PaymentTransaction.TransactionStatus.PROCESSING);
                transactionRepo.save(transaction);

                response.sendRedirect(paymentUrl);
                return;
            }

            // ================== CART (ALL / SELECTED) ==================
            Cart cart = cartService.getCartWithItems(account);
            var items = cart.getItems();

            if (items == null || items.isEmpty()) {
                response.sendRedirect("/cart/view?error=empty_cart");
                return;
            }

            @SuppressWarnings("unchecked")
            List<Long> selectedIds = (List<Long>) request.getSession().getAttribute("CHECKOUT_SELECTED_IDS");

            List<CartItem> payItems = items;
            if (selectedIds != null && !selectedIds.isEmpty()) {
                Set<Long> idSet = new HashSet<>(selectedIds);
                payItems = items.stream()
                        .filter(ci -> ci.getCartItemId() != null && idSet.contains(ci.getCartItemId()))
                        .toList();

                if (payItems.isEmpty()) {
                    response.sendRedirect("/cart/view?error=selected_empty");
                    return;
                }
            }

            double total = payItems.stream().mapToDouble(CartItem::getTotalPrice).sum();

            String txnRef = vnPayUtil.generateTxnRef();
            PaymentTransaction transaction = new PaymentTransaction();
            transaction.setAccount(account);
            transaction.setTransactionType(PaymentTransaction.TransactionType.ORDER_PAYMENT);
            transaction.setStatus(PaymentTransaction.TransactionStatus.PENDING);
            transaction.setAmount(BigDecimal.valueOf(total));
            transaction.setVnpayTxnRef(txnRef);
            transaction.setIpAddress(vnPayUtil.getIpAddress(request));
            transaction.setUserAgent(request.getHeader("User-Agent"));

            if (selectedIds != null && !selectedIds.isEmpty()) {
                String idsCsv = selectedIds.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");
                transaction.setDescription("CHECKOUT_SELECTED:" + cart.getCartId() + ":" + idsCsv);
            } else {
                transaction.setDescription("CHECKOUT_" + cart.getCartId());
            }

            transaction = transactionRepo.save(transaction);

            // ✅ tạo order đúng payItems
            orderService.createOrdersFromCartItems(account, payItems, transaction);

            String orderInfo = "CHECKOUT_" + transaction.getTransactionId();
            String paymentUrl = vnPayUtil.createPaymentUrl(
                    (long) total,
                    orderInfo,
                    txnRef,
                    returnUrlCheckout,
                    request
            );

            if (paymentUrl == null) {
                response.sendRedirect("/checkout?error=create_url_failed");
                return;
            }

            transaction.setStatus(PaymentTransaction.TransactionStatus.PROCESSING);
            transactionRepo.save(transaction);

            // clear selected để tránh dính
            request.getSession().removeAttribute("CHECKOUT_SELECTED_IDS");

            response.sendRedirect(paymentUrl);

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("/checkout?error=" + e.getMessage());
        }
    }

    @GetMapping("/success")
    public String checkoutSuccess(Model model, HttpServletRequest request) {
        Account account = (Account) request.getSession().getAttribute("loggedInAccount");
        if (account == null) return "redirect:/login";

        request.getSession().removeAttribute("BUY_NOW_TOOL_ID");
        request.getSession().removeAttribute("BUY_NOW_LICENSE_ID");
        request.getSession().removeAttribute("BUY_NOW_QTY");
        request.getSession().removeAttribute("CHECKOUT_SELECTED_IDS");

        model.addAttribute("account", account);
        return "checkout/success";
    }
}
