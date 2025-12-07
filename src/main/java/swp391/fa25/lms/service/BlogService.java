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
     * Lấy blog theo ID
     */
    BlogDetailDTO getBlogById(Long blogId);

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
     * Search blog theo keyword
     */
    Page<BlogListItemDTO> searchBlogs(String keyword, Pageable pageable);

    /**
     * Lấy top blog theo view count
     */
    List<BlogListItemDTO> getTopViewedBlogs(int limit);
}
