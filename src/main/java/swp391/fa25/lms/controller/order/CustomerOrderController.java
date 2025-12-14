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
        String email = auth.getName();
        Account acc = accountRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy account của user đang đăng nhập"));
        return acc.getAccountId();
    }

    @GetMapping
    public String myOrders(Authentication auth,
                           Model model,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "10") int size,
                           @RequestParam(required = false) String q,
                           @RequestParam(required = false) String status,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
                           @RequestParam(defaultValue = "createdAt") String sort,
                           @RequestParam(defaultValue = "desc") String dir) {

        Long accountId = currentAccountId(auth);

        CustomerOrder.OrderStatus st = null;
        if (org.springframework.util.StringUtils.hasText(status)) {
            st = CustomerOrder.OrderStatus.valueOf(status);
        }

        model.addAttribute("pageData", orderService.getMyOrders(accountId, q, st, from, to, page, size, sort, dir));

        model.addAttribute("q", q);
        model.addAttribute("statusStr", status == null ? "" : status);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);
        model.addAttribute("size", size);

        return "order/list";
    }


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

        return "order/detail";
    }

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

        // Redirect sang view license account (UC45)
        return "redirect:/customer/license-accounts/" + order.getLicenseAccount().getLicenseAccountId()
                + "?backOrderId=" + orderId;
    }
}
