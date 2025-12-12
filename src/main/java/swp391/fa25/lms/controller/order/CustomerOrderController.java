package swp391.fa25.lms.controller.order;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.CustomerOrder;
import swp391.fa25.lms.repository.AccountRepository;
import swp391.fa25.lms.service.CustomerOrderService;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/customer/orders")
public class CustomerOrderController {

    private final CustomerOrderService orderService;
    private final AccountRepository accountRepo;

    public CustomerOrderController(CustomerOrderService orderService, AccountRepository accountRepo) {
        this.orderService = orderService;
        this.accountRepo = accountRepo;
    }

    private Long currentAccountId(Authentication auth) {
        String email = auth.getName(); // thường là email (username)
        Account acc = accountRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy account của user đang đăng nhập"));
        return acc.getAccountId();
    }

    // 1) View Order List/History
    @GetMapping
    public String myOrders(Authentication auth,
                           Model model,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "10") int size,
                           @RequestParam(required = false) CustomerOrder.OrderStatus status,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        Long accountId = currentAccountId(auth);

        model.addAttribute("pageData", orderService.getMyOrders(accountId, status, from, to, page, size));
        model.addAttribute("status", status);
        model.addAttribute("from", from);
        model.addAttribute("to", to);

        return "order/list"; // Thymeleaf view
    }

    // 2) View Detail Order
    @GetMapping("/{orderId}")
    public String orderDetail(@PathVariable Long orderId,
                              Authentication auth,
                              Model model) {
        Long accountId = currentAccountId(auth);

        CustomerOrder order = orderService.getMyOrderDetail(accountId, orderId);
        model.addAttribute("order", order);

        // hiển thị message VNPay (nếu có)
        if (order.getTransaction() != null) {
            model.addAttribute("paymentMsg", order.getTransaction().getVnpayResponseMessage());
        }

        return "order/detail";
    }
}
