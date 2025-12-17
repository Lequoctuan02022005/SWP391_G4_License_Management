package swp391.fa25.lms.controller.order;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.CustomerOrder;
import swp391.fa25.lms.model.Feedback;
import swp391.fa25.lms.model.PaymentTransaction;
import swp391.fa25.lms.repository.AccountRepository;
import swp391.fa25.lms.repository.CustomerOrderRepository;
import swp391.fa25.lms.repository.FeedbackRepository;
import swp391.fa25.lms.repository.PaymentTransactionRepository;
import swp391.fa25.lms.service.CustomerOrderService;
import swp391.fa25.lms.util.VNPayUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Controller
@RequestMapping("/customer/orders")
public class CustomerOrderController {

    private final CustomerOrderService orderService;
    private final AccountRepository accountRepo;
    private final FeedbackRepository feedbackRepo;

    private final PaymentTransactionRepository transactionRepo;
    private final CustomerOrderRepository orderRepo;
    private final VNPayUtil vnPayUtil;

    @Value("${vnpay.returnUrlCheckout}")
    private String returnUrlCheckout;

    @Value("${vnpay.returnUrlRepay:/payment/repay-return}")
    private String returnUrlRepay;

    private static final Set<String> ALLOWED_SORT = Set.of(
            "createdAt", "price", "toolName", "licenseName", "orderId", "orderStatus"
    );

    public CustomerOrderController(CustomerOrderService orderService,
                                   AccountRepository accountRepo,
                                   FeedbackRepository feedbackRepo,
                                   PaymentTransactionRepository transactionRepo,
                                   CustomerOrderRepository orderRepo,
                                   VNPayUtil vnPayUtil) {
        this.orderService = orderService;
        this.accountRepo = accountRepo;
        this.feedbackRepo = feedbackRepo;
        this.transactionRepo = transactionRepo;
        this.orderRepo = orderRepo;
        this.vnPayUtil = vnPayUtil;
    }

    private Long currentAccountId(Authentication auth) {
        String email = auth.getName();
        Account acc = accountRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy account của user đang đăng nhập"));
        return acc.getAccountId();
    }

    // ====================== LIST ======================
    @GetMapping
    public String myOrders(Authentication auth,
                           Model model,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "10") int size,
                           @RequestParam(defaultValue = "") String q,
                           @RequestParam(defaultValue = "") String status,
                           // ✅ đổi sang DATE để khớp input type="date"
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                           @RequestParam(defaultValue = "createdAt") String sort,
                           @RequestParam(defaultValue = "desc") String dir) {

        Long accountId = currentAccountId(auth);

        q = (q == null) ? "" : q.trim();

        CustomerOrder.OrderStatus st = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                st = CustomerOrder.OrderStatus.valueOf(status.trim());
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(BAD_REQUEST, "Invalid status: " + status);
            }
        }

        // ✅ swap nếu from > to
        if (from != null && to != null && from.isAfter(to)) {
            LocalDate tmp = from;
            from = to;
            to = tmp;
        }

        page = Math.max(page, 0);
        size = Math.min(Math.max(size, 5), 50);

        if (sort == null || !ALLOWED_SORT.contains(sort)) sort = "createdAt";
        dir = (dir != null && dir.equalsIgnoreCase("asc")) ? "asc" : "desc";

        // ✅ convert LocalDate -> LocalDateTime để service query theo khoảng thời gian
        LocalDateTime fromDt = (from == null) ? null : from.atStartOfDay();
        LocalDateTime toDt = (to == null) ? null : to.atTime(LocalTime.MAX);

        var pageData = orderService.getMyOrders(accountId, q, st, fromDt, toDt, page, size, sort, dir);

        model.addAttribute("pageData", pageData);
        model.addAttribute("q", q);
        model.addAttribute("statusStr", status == null ? "" : status.trim());
        model.addAttribute("from", from);   // ✅ trả LocalDate về view
        model.addAttribute("to", to);       // ✅ trả LocalDate về view
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);
        model.addAttribute("size", size);

        return "order/list";
    }

    // ====================== DETAIL ======================
    @GetMapping("/{orderId}")
    public String orderDetail(@PathVariable Long orderId,
                              Authentication auth,
                              Model model) {
        Long accountId = currentAccountId(auth);

        CustomerOrder order = orderService.getMyOrderDetail(accountId, orderId);
        model.addAttribute("order", order);

        if (order.getTransaction() != null) {
            model.addAttribute("paymentMsg", order.getTransaction().getVnpayResponseMessage());
        }

        if (order.getTool() != null) {
            Long toolId = order.getTool().getToolId();

            Feedback myFeedback = feedbackRepo
                    .findByOrder_OrderId(orderId)
                    .orElse(null);

            Double avgRating = feedbackRepo.avgRatingByToolId(toolId);

            model.addAttribute("myFeedback", myFeedback);
            model.addAttribute("avgRating", avgRating);
        }

        return "order/detail";
    }

    // ====================== USE SERVICE ======================
    @GetMapping("/{orderId}/license-account")
    public String useService(@PathVariable Long orderId,
                             Authentication auth,
                             Model model) {
        Long accountId = currentAccountId(auth);

        CustomerOrder order = orderService.getMyOrderDetail(accountId, orderId);

        if (order.getOrderStatus() != CustomerOrder.OrderStatus.SUCCESS || order.getLicenseAccount() == null) {
            model.addAttribute("msg", "MSG-25: No active license found for this order.");
            return "license/account/error";
        }

        return "redirect:/customer/license-accounts/" + order.getLicenseAccount().getLicenseAccountId()
                + "?backOrderId=" + orderId;
    }

    // ====================== ✅ REPAY (BỎ CHẶN) ======================
    @PostMapping("/{orderId}/repay")
    @Transactional
    public String repay(@PathVariable Long orderId,
                        Authentication auth,
                        HttpServletRequest request,
                        RedirectAttributes ra) {

        Long accountId = currentAccountId(auth);
        CustomerOrder order = orderService.getMyOrderDetail(accountId, orderId);

        // ✅ quay lại đúng trang list đang đứng (giữ filter/page)
        String back = request.getHeader("Referer");
        String backToList = (back != null && back.contains("/customer/orders")) ? back : "/customer/orders";

        if (order.getOrderStatus() == CustomerOrder.OrderStatus.SUCCESS) {
            ra.addFlashAttribute("errorMsg", "Đơn này đã thanh toán thành công rồi.");
            return "redirect:" + backToList;
        }

        // ✅ BỎ CHẶN PENDING/PROCESSING: luôn cho repay
        // (anh đã yêu cầu “bỏ chặn”)

        double price = (order.getPrice() == null) ? 0.0 : order.getPrice();
        BigDecimal amount = BigDecimal.valueOf(price).setScale(0, RoundingMode.HALF_UP);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            ra.addFlashAttribute("errorMsg", "Số tiền thanh toán không hợp lệ.");
            return "redirect:" + backToList;
        }

        String txnRef = vnPayUtil.generateTxnRef();

        PaymentTransaction tx = new PaymentTransaction();
        tx.setAccount(order.getAccount());
        tx.setTransactionType(PaymentTransaction.TransactionType.ORDER_PAYMENT);
        tx.setStatus(PaymentTransaction.TransactionStatus.PENDING);
        tx.setAmount(amount);

        // để repay-return parse ra orderId
        tx.setDescription("BUYNOW_REPAY_ORDER_" + orderId);

        tx.setVnpayTxnRef(txnRef);
        tx.setIpAddress(vnPayUtil.getIpAddress(request));
        tx.setUserAgent(request.getHeader("User-Agent"));
        tx = transactionRepo.save(tx);

        order.setTransaction(tx);
        order.setOrderStatus(CustomerOrder.OrderStatus.PENDING);
        order.setLastTxnRef(txnRef);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepo.save(order);

        String orderInfo = "REPAY_" + tx.getTransactionId();
        String paymentUrl = vnPayUtil.createPaymentUrl(
                amount.longValue(),
                orderInfo,
                txnRef,
                returnUrlRepay,
                request
        );

        if (paymentUrl == null) {
            tx.setStatus(PaymentTransaction.TransactionStatus.FAILED);
            transactionRepo.save(tx);
            ra.addFlashAttribute("errorMsg", "Không tạo được link thanh toán VNPay.");
            return "redirect:" + backToList;
        }

        tx.setStatus(PaymentTransaction.TransactionStatus.PROCESSING);
        transactionRepo.save(tx);

        return "redirect:" + paymentUrl;
    }


}
