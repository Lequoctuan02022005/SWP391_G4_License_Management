package swp391.fa25.lms.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.fa25.lms.model.*;
import swp391.fa25.lms.repository.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FeedbackReplyService {

    private final FeedbackReplyRepository feedbackReplyRepo;
    private final FeedbackRepository feedbackRepo;
    private final AccountRepository accountRepo;
    private final ToolRepository toolRepo;

    private static final int MAX_REPLY_LENGTH = 500;

    public Page<Feedback> getSellerToolFeedbacks(Long sellerId, int page, int size, 
                                                   Long toolId, Boolean hasReply, Integer minRating) {
        List<Tool> sellerTools = getSellerTools(sellerId);

        if (sellerTools.isEmpty()) {
            return Page.empty();
        }

        List<Long> sellerToolIds = sellerTools.stream()
                .map(Tool::getToolId)
                .toList();

        List<Feedback> allFeedbacks = feedbackRepo.findAllWithAccountAndTool();
        List<Feedback> filteredFeedbacks = filterFeedbacks(allFeedbacks, sellerToolIds, toolId, hasReply, minRating);

        return createPage(filteredFeedbacks, page, size);
    }

    public Optional<FeedbackReply> getReplyByFeedbackId(Long feedbackId) {
        return feedbackReplyRepo.findByFeedbackId(feedbackId);
    }

    @Transactional
    public FeedbackReply createReply(Long feedbackId, Long sellerId, String content) {
        validateReplyContent(content);

        Feedback feedback = feedbackRepo.findById(feedbackId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy feedback!"));
        Account seller = accountRepo.findById(sellerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy seller!"));

        validateSellerOwnership(feedback, sellerId);
        validateNoExistingReply(feedback);
        validateFeedbackStatus(feedback);

        FeedbackReply reply = new FeedbackReply();
        reply.setFeedback(feedback);
        reply.setSeller(seller);
        reply.setContent(content.trim());
        reply.setCreatedAt(LocalDateTime.now());

        return feedbackReplyRepo.save(reply);
    }

    @Transactional
    public FeedbackReply updateReply(Long replyId, Long sellerId, String content) {
        validateReplyContent(content);

        FeedbackReply reply = feedbackReplyRepo.findById(replyId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy reply!"));

        validateReplyOwnership(reply, sellerId);

        reply.setContent(content.trim());
        reply.setUpdatedAt(LocalDateTime.now());

        return feedbackReplyRepo.save(reply);
    }

    @Transactional
    public void deleteReply(Long replyId, Long sellerId) {
        FeedbackReply reply = feedbackReplyRepo.findById(replyId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy reply!"));

        validateReplyOwnership(reply, sellerId);

        feedbackReplyRepo.delete(reply);
    }

    public long countUnrepliedFeedbacks(Long sellerId) {
        List<Tool> sellerTools = getSellerTools(sellerId);
        List<Long> sellerToolIds = sellerTools.stream()
                .map(Tool::getToolId)
                .toList();

        return feedbackRepo.findAllWithAccountAndTool().stream()
                .filter(fb -> sellerToolIds.contains(fb.getTool().getToolId()))
                .filter(fb -> !feedbackReplyRepo.existsByFeedback(fb))
                .count();
    }

    private List<Tool> getSellerTools(Long sellerId) {
        return toolRepo.findAll().stream()
                .filter(t -> t.getSeller() != null && t.getSeller().getAccountId().equals(sellerId))
                .toList();
    }

    private List<Feedback> filterFeedbacks(List<Feedback> allFeedbacks, List<Long> sellerToolIds,
                                           Long toolId, Boolean hasReply, Integer minRating) {
        return allFeedbacks.stream()
                .filter(fb -> fb.getTool() != null && sellerToolIds.contains(fb.getTool().getToolId()))
                .filter(fb -> toolId == null || fb.getTool().getToolId().equals(toolId))
                .filter(fb -> {
                    if (hasReply == null) return true;
                    boolean fbHasReply = feedbackReplyRepo.existsByFeedback(fb);
                    return hasReply == fbHasReply;
                })
                .filter(fb -> minRating == null || fb.getRating() >= minRating)
                .toList();
    }

    private Page<Feedback> createPage(List<Feedback> feedbacks, int page, int size) {
        int start = Math.min(page * size, feedbacks.size());
        int end = Math.min(start + size, feedbacks.size());
        List<Feedback> pageContent = feedbacks.subList(start, end);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return new PageImpl<>(pageContent, pageable, feedbacks.size());
    }

    private void validateReplyContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Nội dung reply không được để trống!");
        }
        if (content.length() > MAX_REPLY_LENGTH) {
            throw new IllegalArgumentException("Nội dung reply không được vượt quá " + MAX_REPLY_LENGTH + " ký tự!");
        }
    }

    private void validateSellerOwnership(Feedback feedback, Long sellerId) {
        if (!feedback.getTool().getSeller().getAccountId().equals(sellerId)) {
            throw new IllegalStateException("Bạn không có quyền reply feedback này!");
        }
    }

    private void validateNoExistingReply(Feedback feedback) {
        if (feedbackReplyRepo.existsByFeedback(feedback)) {
            throw new IllegalStateException("Feedback này đã có reply! Vui lòng cập nhật reply cũ.");
        }
    }

    private void validateFeedbackStatus(Feedback feedback) {
        if (feedback.getStatus() != Feedback.Status.PUBLISHED && feedback.getStatus() != null) {
            throw new IllegalStateException("Chỉ có thể reply cho feedback đã công khai!");
        }
    }

    private void validateReplyOwnership(FeedbackReply reply, Long sellerId) {
        if (!reply.getSeller().getAccountId().equals(sellerId)) {
            throw new IllegalStateException("Bạn không có quyền thao tác reply này!");
        }
    }
}
