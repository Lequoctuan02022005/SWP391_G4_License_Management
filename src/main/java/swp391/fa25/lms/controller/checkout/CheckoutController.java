package swp391.fa25.lms.controller.checkout;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Cart;
import swp391.fa25.lms.model.CustomerOrder;
import swp391.fa25.lms.model.PaymentTransaction;
import swp391.fa25.lms.repository.PaymentTransactionRepository;
import swp391.fa25.lms.service.CartService;
import swp391.fa25.lms.service.OrderService;
import swp391.fa25.lms.util.VNPayUtil;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/checkout")
public class CheckoutController {

    private final CartService cartService;
    private final OrderService orderService;
    private final PaymentTransactionRepository transactionRepo;
    private final VNPayUtil vnPayUtil;
    
    @Value("${vnpay.returnUrlCheckout}")
    private String returnUrlCheckout;

    @GetMapping
    public String showCheckout(Model model, HttpServletRequest request) {

        Account account = (Account) request.getSession().getAttribute("loggedInAccount");
        if (account == null) {
            return "redirect:/login";
        }

        var cart = cartService.getCartWithItems(account);
        var items = cart.getItems();

        if (items == null || items.isEmpty()) {
            return "redirect:/cart/view";
        }

        // Tính tổng tiền
        double total = items.stream()
                .mapToDouble(item -> item.getTotalPrice())
                .sum();

        model.addAttribute("account", account);
        model.addAttribute("items", items);
        model.addAttribute("total", total);

        return "checkout/confirm";
    }

    @PostMapping("/payment")
    @Transactional
    public void performPayment(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Account account = (Account) request.getSession().getAttribute("loggedInAccount");
        if (account == null) {
            response.sendRedirect("/login");
            return;
        }

        try {
            Cart cart = cartService.getCartWithItems(account);
            var items = cart.getItems();

            if (items == null || items.isEmpty()) {
                response.sendRedirect("/cart/view?error=empty_cart");
                return;
            }

            // Tính tổng tiền
            double total = items.stream()
                    .mapToDouble(item -> item.getTotalPrice())
                    .sum();

            // Tạo PaymentTransaction
            String txnRef = vnPayUtil.generateTxnRef();
            PaymentTransaction transaction = new PaymentTransaction();
            transaction.setAccount(account);
            transaction.setTransactionType(PaymentTransaction.TransactionType.ORDER_PAYMENT);
            transaction.setStatus(PaymentTransaction.TransactionStatus.PENDING);
            transaction.setAmount(BigDecimal.valueOf(total));
            transaction.setDescription("CHECKOUT_" + cart.getCartId());
            transaction.setVnpayTxnRef(txnRef);
            transaction.setIpAddress(vnPayUtil.getIpAddress(request));
            transaction.setUserAgent(request.getHeader("User-Agent"));
            transaction = transactionRepo.save(transaction);

            // Tạo các CustomerOrders từ CartItems
            List<CustomerOrder> orders = orderService.createOrdersFromCart(cart, transaction);

            // Tạo VNPay URL
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

            // Update transaction status
            transaction.setStatus(PaymentTransaction.TransactionStatus.PROCESSING);
            transactionRepo.save(transaction);

            // Redirect to VNPay
            response.sendRedirect(paymentUrl);

        } catch (Exception e) {
            System.err.println("Payment error: " + e.getMessage());
            e.printStackTrace();
            response.sendRedirect("/checkout?error=" + e.getMessage());
        }
    }

    @GetMapping("/success")
    public String checkoutSuccess(Model model, HttpServletRequest request) {
        Account account = (Account) request.getSession().getAttribute("loggedInAccount");
        if (account == null) {
            return "redirect:/login";
        }

        model.addAttribute("account", account);
        return "checkout/success";
    }
}