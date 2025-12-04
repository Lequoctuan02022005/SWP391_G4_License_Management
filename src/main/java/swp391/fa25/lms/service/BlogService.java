package swp391.fa25.lms.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import swp391.fa25.lms.dto.blog.*;
import swp391.fa25.lms.model.Blog;

import java.util.List;

public interface BlogService {

    /**
     * Tạo blog mới
     */
    BlogDetailDTO createBlog(CreateBlogDTO dto, Long authorId);

    /**
     * Cập nhật blog
     */
    BlogDetailDTO updateBlog(UpdateBlogDTO dto, Long authorId);

    /**
     * Xóa blog (soft delete - chuyển về ARCHIVED)
     */
    void deleteBlog(Long blogId, Long authorId);

    /**
     * Xóa vĩnh viễn blog (hard delete)
     */
    void permanentlyDeleteBlog(Long blogId, Long authorId);

    /**
     * Lấy blog theo ID
     */
    BlogDetailDTO getBlogById(Long blogId);

    /**
     * Lấy blog theo slug (cho public view)
     */
    BlogDetailDTO getBlogBySlug(String slug);

    /**
     * Lấy blog theo slug và tăng view count
     */
    BlogDetailDTO getBlogBySlugAndIncrementView(String slug);

    /**
     * Publish blog
     */
    BlogDetailDTO publishBlog(Long blogId, Long authorId);

    /**
     * Unpublish blog (chuyển về DRAFT)
     */
    BlogDetailDTO unpublishBlog(Long blogId, Long authorId);

    /**
     * Archive blog
     */
    BlogDetailDTO archiveBlog(Long blogId, Long authorId);

    /**
     * Lấy danh sách blog với pagination
     */
    Page<BlogListItemDTO> getAllBlogs(Pageable pageable);

    /**
     * Lấy danh sách blog published (public)
     */
    Page<BlogListItemDTO> getPublishedBlogs(Pageable pageable);

    /**
     * Lấy danh sách blog theo category
     */
    Page<BlogListItemDTO> getBlogsByCategory(Long categoryId, Pageable pageable);

    /**
     * Lấy danh sách blog theo author
     */
    Page<BlogListItemDTO> getBlogsByAuthor(Long authorId, Pageable pageable);

    /**
     * Lấy danh sách blog featured
     */
    Page<BlogListItemDTO> getFeaturedBlogs(Pageable pageable);

    /**
     * Search blog theo keyword
     */
    Page<BlogListItemDTO> searchBlogs(String keyword, Pageable pageable);

    /**
     * Advanced search với nhiều filters
     */
    Page<BlogListItemDTO> advancedSearchBlogs(BlogSearchRequestDTO searchRequest);

    /**
     * Lấy top blog theo view count
     */
    List<BlogListItemDTO> getTopViewedBlogs(int limit);

    /**
     * Lấy blog liên quan (cùng category)
     */
    List<BlogListItemDTO> getRelatedBlogs(Long blogId, int limit);

    /**
     * Tăng view count
     */
    void incrementViewCount(Long blogId);

    /**
     * Đếm số blog theo status
     */
    long countBlogsByStatus(Blog.Status status);

    /**
     * Đếm số blog theo category
     */
    long countBlogsByCategory(Long categoryId);

    /**
     * Đếm số blog theo author
     */
    long countBlogsByAuthor(Long authorId);

    /**
     * Kiểm tra slug đã tồn tại chưa
     */
    boolean isSlugExists(String slug);

    /**
     * Generate unique slug từ title
     */
    String generateUniqueSlug(String title);
}
