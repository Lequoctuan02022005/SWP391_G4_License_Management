package swp391.fa25.lms.dto.blog;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO để hiển thị chi tiết blog (trang detail)
 * Chứa đầy đủ thông tin bao gồm content
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlogDetailDTO {

    private Long blogId;
    private String title;
    private String slug;
    private String summary;
    private String content;
    private String thumbnailImage;
    private String bannerImage;
    private String status;
    private Boolean featured;
    private Boolean allowComments;
    private Integer viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime scheduledPublishAt;

    // Category info
    private Long categoryId;
    private String categoryName;
    private String categorySlug;
    private String categoryIcon;

    // Author info
    private Long authorId;
    private String authorName;
    private String authorEmail;
    private String authorAvatar;

    // Related blogs (cùng category)
    private List<BlogListItemDTO> relatedBlogs;

    // Computed fields
    private Integer readingTime; // Thời gian đọc ước tính (phút)
    private String formattedCreatedAt; // Format: "5 giờ trước", "2 ngày trước"

    /**
     * Constructor từ entity Blog
     */
    public BlogDetailDTO(swp391.fa25.lms.model.Blog blog) {
        this.blogId = blog.getBlogId();
        this.title = blog.getTitle();
        this.slug = blog.getSlug();
        this.summary = blog.getSummary();
        this.content = blog.getContent();
        this.thumbnailImage = blog.getThumbnailImage();
        this.bannerImage = blog.getBannerImage();
        this.status = blog.getStatus() != null ? blog.getStatus().name() : null;
        this.featured = blog.getFeatured();
        this.allowComments = blog.getAllowComments();
        this.viewCount = blog.getViewCount();
        this.createdAt = blog.getCreatedAt();
        this.updatedAt = blog.getUpdatedAt();
        this.scheduledPublishAt = blog.getScheduledPublishAt();

        if (blog.getCategory() != null) {
            this.categoryId = blog.getCategory().getCategoryId();
            this.categoryName = blog.getCategory().getCategoryName();
            this.categorySlug = blog.getCategory().getSlug();
            this.categoryIcon = blog.getCategory().getIcon();
        }

        if (blog.getAuthor() != null) {
            this.authorId = blog.getAuthor().getAccountId();
            this.authorName = blog.getAuthor().getFullName();
            this.authorEmail = blog.getAuthor().getEmail();
            // authorAvatar có thể lấy từ Account nếu có field
        }

        // Estimate reading time
        if (this.content != null) {
            int wordCount = this.content.split("\\s+").length;
            this.readingTime = Math.max(1, wordCount / 200);
        } else {
            this.readingTime = 1;
        }

        // Format created at (sẽ được xử lý ở service layer)
        this.formattedCreatedAt = formatTimeAgo(this.createdAt);
    }

    /**
     * Helper method để format thời gian "x giờ trước", "x ngày trước"
     */
    private String formatTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) return "";

        LocalDateTime now = LocalDateTime.now();
        long minutes = java.time.Duration.between(dateTime, now).toMinutes();

        if (minutes < 60) {
            return minutes + " phút trước";
        } else if (minutes < 1440) { // < 24 hours
            return (minutes / 60) + " giờ trước";
        } else if (minutes < 43200) { // < 30 days
            return (minutes / 1440) + " ngày trước";
        } else {
            return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }
    }
}
