package swp391.fa25.lms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.Feedback;
import swp391.fa25.lms.model.Tool;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    Optional<Feedback> findByOrder_OrderId(Long orderId);

    @Query("SELECT f FROM Feedback f LEFT JOIN FETCH f.account LEFT JOIN FETCH f.tool ORDER BY f.createdAt DESC")
    List<Feedback> findAllWithAccountAndTool();

    @Query("""
        SELECT AVG(f.rating) FROM Feedback f 
        WHERE f.tool = :tool 
        AND (f.status = :status OR f.status IS NULL)
    """)
    Double avgRatingByToolAndStatus(@Param("tool") Tool tool, @Param("status") Feedback.Status status);

    @Query("""
        SELECT COUNT(f) FROM Feedback f 
        WHERE f.tool = :tool 
        AND (f.status = :status OR f.status IS NULL)
    """)
    long countByToolAndStatus(@Param("tool") Tool tool, @Param("status") Feedback.Status status);

    @Query("""
        SELECT f FROM Feedback f 
        WHERE f.tool = :tool 
        AND (f.status = :status OR f.status IS NULL)
    """)
    Page<Feedback> findByToolAndStatus(@Param("tool") Tool tool, 
                                        @Param("status") Feedback.Status status, 
                                        Pageable pageable);

    @Query("SELECT AVG(f.rating) FROM Feedback f WHERE f.tool.toolId = :toolId")
    Double avgRatingByToolId(@Param("toolId") Long toolId);
}
