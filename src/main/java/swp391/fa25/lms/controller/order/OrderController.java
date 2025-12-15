package swp391.fa25.lms.controller.order;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.CustomerOrder;
import swp391.fa25.lms.repository.OrderRepository;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderController {

    private final OrderRepository orderRepository;

    @GetMapping
    public String listOrders(HttpSession session, Model model) {
        Account account = (Account) session.getAttribute("loggedInAccount");
        if (account == null) {
            return "redirect:/login";
        }

        List<CustomerOrder> orders = orderRepository.findByAccount_AccountId(account.getAccountId());
        
        // Sắp xếp theo thời gian tạo mới nhất
        orders.sort((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()));

        model.addAttribute("orders", orders);
        model.addAttribute("account", account);
        
        return "order/list";
    }

    @GetMapping("/{orderId}")
    public String viewOrderDetail(@PathVariable Long orderId, HttpSession session, Model model) {
        Account account = (Account) session.getAttribute("loggedInAccount");
        if (account == null) {
            return "redirect:/login";
        }

        CustomerOrder order = orderRepository.findById(orderId).orElse(null);
        
        if (order == null) {
            model.addAttribute("error", "Không tìm thấy đơn hàng!");
            return "redirect:/orders";
        }

        // Kiểm tra quyền: chỉ cho xem đơn hàng của mình
        if (!order.getAccount().getAccountId().equals(account.getAccountId())) {
            model.addAttribute("error", "Bạn không có quyền xem đơn hàng này!");
            return "redirect:/orders";
        }

        model.addAttribute("order", order);
        model.addAttribute("account", account);
        
        return "order/detail";
    }
}
