package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.BlogCategory;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlogCategoryRepository extends JpaRepository<BlogCategory, Long> {

    /**
     * Tìm category theo slug (dùng cho URL SEO-friendly)
     */
    Optional<BlogCategory> findBySlug(String slug);

    /**
     * Kiểm tra xem category name đã tồn tại chưa (case-insensitive)
     */
    boolean existsByCategoryNameIgnoreCase(String categoryName);

    /**
     * Lấy tất cả category ACTIVE và sắp xếp theo displayOrder
     */
    @Query("SELECT c FROM BlogCategory c WHERE c.status = 'ACTIVE' ORDER BY c.displayOrder ASC, c.categoryName ASC")
    List<BlogCategory> findAllActiveOrderByDisplayOrder();

    /**
     * Lấy tất cả category sắp xếp theo displayOrder
     */
    @Query("SELECT c FROM BlogCategory c ORDER BY c.displayOrder ASC, c.categoryName ASC")
    List<BlogCategory> findAllOrderByDisplayOrder();

    /**
     * Lấy displayOrder lớn nhất (để tạo category mới)
     */
    @Query("SELECT MAX(c.displayOrder) FROM BlogCategory c")
    Integer findMaxDisplayOrder();

    /**
     * Search và sort categories
     */
    @Query("SELECT c FROM BlogCategory c WHERE " +
            "(:keyword IS NULL OR LOWER(c.categoryName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.slug) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:status IS NULL OR c.status = :status) " +
            "ORDER BY " +
            "CASE WHEN :sortBy = 'categoryName' THEN c.categoryName END ASC, " +
            "CASE WHEN :sortBy = 'displayOrder' THEN c.displayOrder END ASC, " +
            "CASE WHEN :sortBy = 'createdAt' THEN c.createdAt END ASC, " +
            "CASE WHEN :sortBy = 'status' THEN c.status END ASC")
    List<BlogCategory> searchAndSort(@Param("keyword") String keyword,
                                     @Param("status") BlogCategory.Status status,
                                     @Param("sortBy") String sortBy);
}
