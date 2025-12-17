package swp391.fa25.lms.controller.feedback;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.CustomerOrder;
import swp391.fa25.lms.model.Feedback;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.repository.OrderRepository;
import swp391.fa25.lms.repository.ToolRepository;
import swp391.fa25.lms.service.FeedbackService;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
@RequestMapping("/customer/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final OrderRepository orderRepo;
    private final ToolRepository toolRepo;

    @GetMapping("/create")
    public String showFeedbackForm(@RequestParam Long orderId,
                                   @RequestParam Long toolId,
                                   HttpSession session,
                                   Model model) {
        Account account = (Account) session.getAttribute("loggedInAccount");
        if (account == null) {
            return "redirect:/login";
        }

        Optional<CustomerOrder> orderOpt = orderRepo.findById(orderId);
        if (orderOpt.isEmpty() || !orderOpt.get().getAccount().getAccountId().equals(account.getAccountId())) {
            model.addAttribute("error", "Không tìm thấy đơn hàng!");
            return "redirect:/orders";
        }

        CustomerOrder order = orderOpt.get();

        if (order.getOrderStatus() != CustomerOrder.OrderStatus.SUCCESS) {
            model.addAttribute("error", "Chỉ có thể đánh giá đơn hàng đã thanh toán thành công!");
            return "redirect:/orders/" + orderId;
        }

        Tool tool = toolRepo.findById(toolId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tool!"));

        Optional<Feedback> existingFeedback = feedbackService.findFeedbackByOrder(orderId);

        model.addAttribute("order", order);
        model.addAttribute("tool", tool);
        model.addAttribute("existingFeedback", existingFeedback.orElse(null));
        model.addAttribute("account", account);

        return "feedback/feedback";
    }

    @PostMapping("/create")
    public String submitFeedback(@RequestParam Long orderId,
                                 @RequestParam Long toolId,
                                 @RequestParam Integer rating,
                                 @RequestParam(required = false) String comment,
                                 HttpSession session,
                                 RedirectAttributes ra) {
        Account account = (Account) session.getAttribute("loggedInAccount");
        if (account == null) {
            return "redirect:/login";
        }

        try {
            feedbackService.submitFeedback(orderId, account.getAccountId(), toolId, rating, comment);
            ra.addFlashAttribute("success", "Đánh giá của bạn đã được ghi nhận! Cảm ơn bạn đã chia sẻ.");
            return "redirect:/orders/" + orderId;
        } catch (IllegalStateException | IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/customer/feedback/create?orderId=" + orderId + "&toolId=" + toolId;
        }
    }
}
