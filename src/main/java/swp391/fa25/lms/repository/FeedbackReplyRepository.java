package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.Feedback;
import swp391.fa25.lms.model.FeedbackReply;

import java.util.Optional;

@Repository
public interface FeedbackReplyRepository extends JpaRepository<FeedbackReply, Long> {

    Optional<FeedbackReply> findByFeedback(Feedback feedback);

    @Query("SELECT fr FROM FeedbackReply fr LEFT JOIN FETCH fr.seller WHERE fr.feedback.feedbackId = :feedbackId")
    Optional<FeedbackReply> findByFeedbackId(@Param("feedbackId") Long feedbackId);

    boolean existsByFeedback(Feedback feedback);

    @Query("SELECT COUNT(fr) FROM FeedbackReply fr WHERE fr.seller.accountId = :sellerId")
    long countBySellerId(@Param("sellerId") Long sellerId);
}
