package swp391.fa25.lms.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Feedback;
import swp391.fa25.lms.model.Tool;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    @Query("""
           select avg(f.rating)
           from Feedback f
           where f.tool = :tool
             and (f.status = :status or f.status is null)
           """)
    Double avgRatingByToolAndStatusOrNull(@Param("tool") Tool tool,
                                          @Param("status") Feedback.Status status);

    @Query("""
           select count(f)
           from Feedback f
           where f.tool = :tool
             and (f.status = :status or f.status is null)
           """)
    long countByToolAndStatusOrNull(@Param("tool") Tool tool,
                                    @Param("status") Feedback.Status status);
    @Query("""
           select f
           from Feedback f
           where f.tool = :tool
             and (f.status = :status or f.status is null)
           """)
    Page<Feedback> findByToolAndStatusOrNull(@Param("tool") Tool tool,
                                             @Param("status") Feedback.Status status,
                                             Pageable pageable);
}
