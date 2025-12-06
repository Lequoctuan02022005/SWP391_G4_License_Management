package swp391.fa25.lms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Tool;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ToolRepository extends JpaRepository<Tool, Long> {
    Optional<Tool> findByToolIdAndSeller(Long toolId, Account seller);

    boolean existsByToolName(String toolName);

    //mod
    // MODERATOR – find pending tools
    @Query("SELECT t FROM Tool t " +
            "WHERE t.status = swp391.fa25.lms.model.Tool.Status.PENDING " +
            "AND (:keyword IS NULL OR t.toolName LIKE %:keyword%)")
    Page<Tool> findPendingUploads(
            @Param("keyword") String keyword, Pageable pageable
    );

    // MANAGER – filter tools
    @Query("SELECT t FROM Tool t " +
            "WHERE (:sellerId IS NULL OR t.seller.accountId = :sellerId) " +
            "AND (:categoryId IS NULL OR t.category.categoryId = :categoryId) " +
            "AND (:status IS NULL OR t.status = :status) " +
            "AND (:name IS NULL OR t.toolName LIKE %:name%) " +
            "AND (:from IS NULL OR t.createdAt >= :from) " +
            "AND (:to IS NULL OR t.createdAt <= :to)")
    Page<Tool> filterToolsForManager(
            @Param("sellerId") Long sellerId,
            @Param("categoryId") Long categoryId,
            @Param("status") Tool.Status status,
            @Param("name") String name,
            @Param("from") LocalDateTime fromDate,
            @Param("to") LocalDateTime toDate,
            Pageable pageable
    );

    // Home page for customers
    @Query("SELECT t FROM Tool t " +
            "WHERE t.status = swp391.fa25.lms.model.Tool.Status.PUBLISHED " +
            "AND (:keyword IS NULL OR t.toolName LIKE %:keyword%)")
    Page<Tool> findPublishedTools(
            @Param("keyword") String keyword, Pageable pageable
    );

    List<Tool> findBySellerAccountId(Long sellerId);

    Page<Tool> findByStatus(Tool.Status status, Pageable pageable);

    @Query("SELECT t.toolName FROM Tool t WHERE t.toolName LIKE %:keyword%")
    List<String> searchNames(@Param("keyword") String keyword);

    @Query("SELECT COUNT(t) FROM Tool t WHERE t.status = :status")
    long countByStatus(@Param("status") Tool.Status status);

    @Query("""
            SELECT t FROM Tool t
            WHERE (:keyword IS NULL OR :keyword = '' 
                   OR LOWER(t.toolName) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Tool> findAllTools(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT t FROM Tool t
            WHERE (:keyword IS NULL OR :keyword = '' 
                   OR LOWER(t.toolName) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:status IS NULL OR t.status = :status)
            """)
    Page<Tool> filterTools(
            @Param("keyword") String keyword,
            @Param("status") Tool.Status status,
            Pageable pageable
    );

    @Query("""
            SELECT t FROM Tool t
            WHERE t.status = 'PUBLISHED'
              AND t.seller.sellerActive = true
              AND (t.seller.sellerExpiryDate IS NULL OR t.seller.sellerExpiryDate >= CURRENT_TIMESTAMP)
            """)
    List<Tool> findAllPublishedAndSellerActive();


    // Lấy Tool theo id status PUBLISHED
    @EntityGraph(attributePaths = {"licenses", "seller", "category"})
    Optional<Tool> findByToolIdAndStatus(Long toolId, Tool.Status status);
}
