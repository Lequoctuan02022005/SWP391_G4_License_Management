package swp391.fa25.lms.controller.order;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.CustomerOrder;
import swp391.fa25.lms.model.Feedback;
import swp391.fa25.lms.repository.AccountRepository;
import swp391.fa25.lms.repository.FeedbackRepository;
import swp391.fa25.lms.service.CustomerOrderService;

import java.time.LocalDateTime;
import java.util.Set;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Controller
@RequestMapping("/customer/orders")
public class CustomerOrderController {

    private final CustomerOrderService orderService;
    private final AccountRepository accountRepo;
    private final FeedbackRepository feedbackRepo;

    private static final Set<String> ALLOWED_SORT = Set.of(
            "createdAt", "price", "toolName", "licenseName", "orderId", "orderStatus"
    );

    public CustomerOrderController(CustomerOrderService orderService,
                                   AccountRepository accountRepo,
                                   FeedbackRepository feedbackRepo) {
        this.orderService = orderService;
        this.accountRepo = accountRepo;
        this.feedbackRepo = feedbackRepo;
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
                           @RequestParam(defaultValue = "") String q,
                           @RequestParam(defaultValue = "") String status,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
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

        if (from != null && to != null && from.isAfter(to)) {
            LocalDateTime tmp = from;
            from = to;
            to = tmp;
        }

        page = Math.max(page, 0);
        size = Math.min(Math.max(size, 5), 50);

        if (sort == null || !ALLOWED_SORT.contains(sort)) sort = "createdAt";
        dir = (dir != null && dir.equalsIgnoreCase("asc")) ? "asc" : "desc";

        var pageData = orderService.getMyOrders(accountId, q, st, from, to, page, size, sort, dir);

        model.addAttribute("pageData", pageData);
        model.addAttribute("q", q);
        model.addAttribute("statusStr", status == null ? "" : status.trim());
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
}
