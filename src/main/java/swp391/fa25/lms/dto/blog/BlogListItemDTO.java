package swp391.fa25.lms.dto.blog;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO để hiển thị danh sách blog (trang list/search)
 * Chỉ chứa thông tin cần thiết, không có full content
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlogListItemDTO {

    private Long blogId;
    private String title;
    private String slug;
    private String summary;
    private String thumbnailImage;
    private String status;
    private Integer viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime scheduledPublishAt;

    // Category info
    private Long categoryId;
    private String categoryName;
    private String categorySlug;

    // Author info
    private Long authorId;
    private String authorName;
    private String authorEmail;

    // Computed fields
    private String shortSummary; // Tóm tắt ngắn (100 ký tự đầu)
    private Integer readingTime; // Thời gian đọc ước tính (phút)

    /**
     * Constructor từ entity Blog để dễ map
     */
    public BlogListItemDTO(swp391.fa25.lms.model.Blog blog) {
        this.blogId = blog.getBlogId();
        this.title = blog.getTitle();
        this.slug = blog.getSlug();
        this.summary = blog.getSummary();
        this.thumbnailImage = blog.getThumbnailImage();
        this.status = blog.getStatus() != null ? blog.getStatus().name() : null;
        this.viewCount = blog.getViewCount();
        this.createdAt = blog.getCreatedAt();
        this.updatedAt = blog.getUpdatedAt();
        this.scheduledPublishAt = blog.getScheduledPublishAt();

        // Safely access lazy-loaded relationships within transaction
        try {
            if (blog.getCategory() != null) {
                this.categoryId = blog.getCategory().getCategoryId();
                this.categoryName = blog.getCategory().getCategoryName();
                this.categorySlug = blog.getCategory().getSlug();
            }
        } catch (Exception e) {
            // Category not loaded - leave null
            this.categoryId = null;
            this.categoryName = null;
            this.categorySlug = null;
        }

        try {
            if (blog.getAuthor() != null) {
                this.authorId = blog.getAuthor().getAccountId();
                this.authorName = blog.getAuthor().getFullName();
                this.authorEmail = blog.getAuthor().getEmail();
            }
        } catch (Exception e) {
            // Author not loaded - leave null
            this.authorId = null;
            this.authorName = null;
            this.authorEmail = null;
        }

        // Generate short summary (100 chars)
        if (this.summary != null && this.summary.length() > 100) {
            this.shortSummary = this.summary.substring(0, 100) + "...";
        } else {
            this.shortSummary = this.summary;
        }

        // Set default reading time (avoid loading content for list view)
        this.readingTime = 5; // Default 5 minutes
    }
}
