package swp391.fa25.lms.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "Feedback")
@Getter
@Setter
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_id")
    private Long feedbackId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    @JsonBackReference(value = "feedback-account")
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tool_id", nullable = false)
    @JsonBackReference(value = "feedback-tool")
    private Tool tool;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonBackReference(value = "feedback-order")
    private CustomerOrder order;

    @Min(1) 
    @Max(5)
    @Column(nullable = false)
    private Integer rating;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(columnDefinition = "NVARCHAR(100)")
    private String comment;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public enum Status {
        PUBLISHED, 
        SUSPECT, 
        HIDDEN
    }
}
