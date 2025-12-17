package swp391.fa25.lms.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.fa25.lms.model.*;
import swp391.fa25.lms.repository.*;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepo;
    private final OrderRepository orderRepo;
    private final AccountRepository accountRepo;
    private final ToolRepository toolRepo;

    private static final int MAX_COMMENT_LENGTH = 100;
    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 5;

    public Optional<Feedback> findFeedbackByOrder(Long orderId) {
        return feedbackRepo.findByOrder_OrderId(orderId);
    }

    @Transactional
    public Feedback submitFeedback(Long orderId, Long accountId, Long toolId, Integer rating, String comment) {
        validateRating(rating);
        validateComment(comment);

        CustomerOrder order = loadAndValidateOrder(orderId, accountId);
        Account account = accountRepo.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản!"));
        Tool tool = toolRepo.findById(toolId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tool!"));

        Optional<Feedback> existingFeedback = findFeedbackByOrder(orderId);

        Feedback feedback;
        if (existingFeedback.isPresent()) {
            feedback = updateExistingFeedback(existingFeedback.get(), rating, comment);
        } else {
            feedback = createNewFeedback(account, tool, order, rating, comment);
        }

        return feedbackRepo.save(feedback);
    }

    public Double getAverageRating(Long toolId) {
        return feedbackRepo.avgRatingByToolId(toolId);
    }

    private void validateRating(Integer rating) {
        if (rating == null || rating < MIN_RATING || rating > MAX_RATING) {
            throw new IllegalArgumentException("Rating phải từ " + MIN_RATING + " đến " + MAX_RATING + "!");
        }
    }

    private void validateComment(String comment) {
        if (comment != null && comment.length() > MAX_COMMENT_LENGTH) {
            throw new IllegalArgumentException("Nhận xét không được vượt quá " + MAX_COMMENT_LENGTH + " ký tự!");
        }
    }

    private CustomerOrder loadAndValidateOrder(Long orderId, Long accountId) {
        CustomerOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng!"));

        if (order.getOrderStatus() != CustomerOrder.OrderStatus.SUCCESS) {
            throw new IllegalStateException("Chỉ có thể đánh giá đơn hàng đã thanh toán thành công!");
        }

        if (!order.getAccount().getAccountId().equals(accountId)) {
            throw new IllegalStateException("Đơn hàng không thuộc về bạn!");
        }

        return order;
    }

    private Feedback updateExistingFeedback(Feedback feedback, Integer rating, String comment) {
        feedback.setRating(rating);
        feedback.setComment(comment);
        return feedback;
    }

    private Feedback createNewFeedback(Account account, Tool tool, CustomerOrder order, Integer rating, String comment) {
        Feedback feedback = new Feedback();
        feedback.setAccount(account);
        feedback.setTool(tool);
        feedback.setOrder(order);
        feedback.setRating(rating);
        feedback.setComment(comment);
        feedback.setCreatedAt(LocalDateTime.now());
        feedback.setStatus(Feedback.Status.PUBLISHED);
        return feedback;
    }
}
