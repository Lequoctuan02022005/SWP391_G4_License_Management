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
     * Tìm category với số lượng blog PUBLISHED (dùng cho public view)
     * Sửa query để đảm bảo tất cả category ACTIVE đều được hiển thị, kể cả không có blog PUBLISHED
     */
    @Query("SELECT c, COUNT(CASE WHEN b.status = 'PUBLISHED' THEN 1 END) " +
            "FROM BlogCategory c LEFT JOIN c.blogs b " +
            "WHERE c.status = 'ACTIVE' " +
            "GROUP BY c ORDER BY c.displayOrder ASC")
    List<Object[]> findCategoriesWithBlogCount();

    /**
     * Đếm số category theo status
     */
    long countByStatus(BlogCategory.Status status);

    /**
     * Lấy displayOrder lớn nhất (để tạo category mới)
     */
    @Query("SELECT MAX(c.displayOrder) FROM BlogCategory c")
    Integer findMaxDisplayOrder();

    /**
     * Tìm categories có displayOrder trong khoảng (để swap order)
     */
    @Query("SELECT c FROM BlogCategory c WHERE c.displayOrder BETWEEN :start AND :end ORDER BY c.displayOrder")
    List<BlogCategory> findByDisplayOrderBetween(@Param("start") Integer start, @Param("end") Integer end);
}
