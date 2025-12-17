package swp391.fa25.lms.controller.feedback;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Feedback;
import swp391.fa25.lms.model.FeedbackReply;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.repository.ToolRepository;
import swp391.fa25.lms.service.FeedbackReplyService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/seller/feedbacks")
@PreAuthorize("hasRole('SELLER')")
public class SellerFeedbackController {

    private final FeedbackReplyService feedbackReplyService;
    private final ToolRepository toolRepo;

    @GetMapping
    @Transactional(readOnly = true)
    public String listFeedbacks(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "10") int size,
                                @RequestParam(required = false) Long toolId,
                                @RequestParam(required = false) Boolean hasReply,
                                @RequestParam(required = false) Integer minRating,
                                HttpSession session,
                                Model model) {
        Account seller = (Account) session.getAttribute("loggedInAccount");
        if (seller == null) {
            return "redirect:/login";
        }

        Page<Feedback> feedbacks = feedbackReplyService.getSellerToolFeedbacks(
                seller.getAccountId(), page, size, toolId, hasReply, minRating);

        List<Tool> sellerTools = toolRepo.findAll().stream()
                .filter(t -> t.getSeller().getAccountId().equals(seller.getAccountId()))
                .toList();

        long unrepliedCount = feedbackReplyService.countUnrepliedFeedbacks(seller.getAccountId());

        Map<Long, FeedbackReply> repliesMap = loadReplies(feedbacks);

        model.addAttribute("feedbacks", feedbacks);
        model.addAttribute("repliesMap", repliesMap);
        model.addAttribute("sellerTools", sellerTools);
        model.addAttribute("unrepliedCount", unrepliedCount);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", feedbacks.getTotalPages());
        model.addAttribute("selectedToolId", toolId);
        model.addAttribute("selectedHasReply", hasReply);
        model.addAttribute("selectedMinRating", minRating);
        model.addAttribute("account", seller);

        return "feedback/feedback-list";
    }

    @PostMapping("/{feedbackId}/reply")
    public String createReply(@PathVariable Long feedbackId,
                              @RequestParam String content,
                              HttpSession session,
                              RedirectAttributes ra) {
        Account seller = (Account) session.getAttribute("loggedInAccount");
        if (seller == null) {
            return "redirect:/login";
        }

        try {
            feedbackReplyService.createReply(feedbackId, seller.getAccountId(), content);
            ra.addFlashAttribute("success", "Reply đã được gửi thành công!");
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/seller/feedbacks";
    }

    @PostMapping("/reply/{replyId}/update")
    public String updateReply(@PathVariable Long replyId,
                              @RequestParam String content,
                              HttpSession session,
                              RedirectAttributes ra) {
        Account seller = (Account) session.getAttribute("loggedInAccount");
        if (seller == null) {
            return "redirect:/login";
        }

        try {
            feedbackReplyService.updateReply(replyId, seller.getAccountId(), content);
            ra.addFlashAttribute("success", "Reply đã được cập nhật!");
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/seller/feedbacks";
    }

    @PostMapping("/reply/{replyId}/delete")
    public String deleteReply(@PathVariable Long replyId,
                              HttpSession session,
                              RedirectAttributes ra) {
        Account seller = (Account) session.getAttribute("loggedInAccount");
        if (seller == null) {
            return "redirect:/login";
        }

        try {
            feedbackReplyService.deleteReply(replyId, seller.getAccountId());
            ra.addFlashAttribute("success", "Reply đã được xóa!");
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/seller/feedbacks";
    }

    private Map<Long, FeedbackReply> loadReplies(Page<Feedback> feedbacks) {
        Map<Long, FeedbackReply> repliesMap = new HashMap<>();
        for (Feedback fb : feedbacks.getContent()) {
            feedbackReplyService.getReplyByFeedbackId(fb.getFeedbackId())
                    .ifPresent(reply -> repliesMap.put(fb.getFeedbackId(), reply));
        }
        return repliesMap;
    }
}
