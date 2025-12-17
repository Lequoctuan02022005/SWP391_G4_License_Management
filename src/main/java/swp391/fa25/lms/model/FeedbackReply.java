package swp391.fa25.lms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "feedback_reply")
@Getter
@Setter
public class FeedbackReply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reply_id")
    private Long replyId;

    @OneToOne
    @JoinColumn(name = "feedback_id", nullable = false, unique = true)
    private Feedback feedback;

    @ManyToOne
    @JoinColumn(name = "seller_id", nullable = false)
    private Account seller;

    @Column(name = "content", columnDefinition = "NVARCHAR(500)", nullable = false)
    private String content;

    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
