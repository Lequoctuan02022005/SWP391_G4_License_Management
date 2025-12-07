package swp391.fa25.lms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.Blog;
import swp391.fa25.lms.model.BlogCategory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BlogRepository extends JpaRepository<Blog, Long> {

    /**
     * Tìm blog theo slug (dùng cho URL SEO-friendly)
     */
    Optional<Blog> findBySlug(String slug);

    /**
     * Kiểm tra xem slug đã tồn tại chưa
     */
    boolean existsBySlug(String slug);

    /**
     * Tìm tất cả blog theo category và status với phân trang
     */
    Page<Blog> findByCategoryAndStatus(BlogCategory category, Blog.Status status, Pageable pageable);

    /**
     * Tìm blog theo author (Manager)
     */
    Page<Blog> findByAuthorAccountId(Long authorId, Pageable pageable);

    /**
     * Search blog theo title hoặc content (full-text search)
     */
    @Query("SELECT b FROM Blog b " +
            "WHERE (LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(b.content) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(b.summary) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "b.status = :status")
    Page<Blog> searchByKeyword(@Param("keyword") String keyword, 
                                @Param("status") Blog.Status status, 
                                Pageable pageable);

    /**
     * Advanced search với nhiều filters
     */
    @Query("SELECT b FROM Blog b " +
            "WHERE (:categoryId IS NULL OR b.category.blogCategoryId = :categoryId) AND " +
            "(:status IS NULL OR b.status = :status) AND " +
            "(:authorId IS NULL OR b.author.accountId = :authorId) AND " +
            "(:keyword IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(b.content) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Blog> advancedSearch(@Param("categoryId") Long categoryId,
                               @Param("status") Blog.Status status,
                               @Param("authorId") Long authorId,
                               @Param("keyword") String keyword,
                               Pageable pageable);

    /**
     * Lấy các blog published và có scheduledPublishAt <= hiện tại
     */
    @Query("SELECT b FROM Blog b " +
            "WHERE b.status = 'PUBLISHED' AND " +
            "(b.scheduledPublishAt IS NULL OR b.scheduledPublishAt <= :now)")
    Page<Blog> findPublishedBlogs(@Param("now") LocalDateTime now, Pageable pageable);

    /**
     * Lấy top blog theo view count
     */
    @Query("SELECT DISTINCT b FROM Blog b " +
            "LEFT JOIN FETCH b.category " +
            "LEFT JOIN FETCH b.author " +
            "WHERE b.status = 'PUBLISHED' " +
            "ORDER BY b.viewCount DESC")
    List<Blog> findTopViewedBlogs(Pageable pageable);

    /**
     * Lấy các blog liên quan (cùng category, khác ID)
     */
    @Query("SELECT DISTINCT b FROM Blog b " +
            "LEFT JOIN FETCH b.category " +
            "LEFT JOIN FETCH b.author " +
            "WHERE b.category.blogCategoryId = :categoryId AND " +
            "b.blogId != :excludeBlogId AND " +
            "b.status = 'PUBLISHED' " +
            "ORDER BY b.createdAt DESC")
    List<Blog> findRelatedBlogs(@Param("categoryId") Long categoryId, 
                                 @Param("excludeBlogId") Long excludeBlogId,
                                 Pageable pageable);

    /**
     * Đếm số blog theo status
     */
    long countByStatus(Blog.Status status);

    /**
     * Đếm số blog theo category (tất cả status)
     */
    long countByCategory(BlogCategory category);

    /**
     * Đếm số blog PUBLISHED theo category (dùng cho public view)
     */
    @Query("SELECT COUNT(b) FROM Blog b WHERE b.category = :category AND b.status = 'PUBLISHED'")
    long countPublishedBlogsByCategory(@Param("category") BlogCategory category);

    /**
     * Đếm số blog theo author
     */
    long countByAuthorAccountId(Long authorId);
}
