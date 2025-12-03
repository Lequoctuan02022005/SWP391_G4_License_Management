package swp391.fa25.lms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Tool;

import java.util.List;
import java.util.Optional;

@Repository
public interface ToolRepository extends JpaRepository<Tool, Long> {

    // Tìm tool theo id và seller (để check quyền edit)
    Optional<Tool> findByToolIdAndSeller(Long toolId, Account seller);

    // Lấy tool của seller (không lấy DEACTIVATED)
    List<Tool> findBySellerAndStatusNot(Account seller, Tool.Status status);

    // Check trùng tên tool
    boolean existsByToolName(String toolName);

    // Tìm kiếm tool theo filter trong seller dashboard
    @Query("""
           SELECT DISTINCT t
           FROM Tool t
           LEFT JOIN t.licenses l
           WHERE t.seller.accountId = :sellerId
             AND (:keyword IS NULL OR LOWER(t.toolName) LIKE LOWER(CONCAT('%', :keyword, '%')))
             AND (:categoryId IS NULL OR t.category.categoryId = :categoryId)
             AND (:status IS NULL OR t.status = :status)
             AND (:loginMethod IS NULL OR t.loginMethod = :loginMethod)
             AND (:minPrice IS NULL OR l.price >= :minPrice)
             AND (:maxPrice IS NULL OR l.price <= :maxPrice)
           """)
    Page<Tool> searchToolsForSeller(
            @Param("sellerId") Long sellerId,
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("status") Tool.Status status,
            @Param("loginMethod") Tool.LoginMethod loginMethod,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            Pageable pageable
    );
}
