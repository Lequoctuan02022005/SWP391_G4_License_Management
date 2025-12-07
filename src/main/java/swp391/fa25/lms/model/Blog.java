package swp391.fa25.lms.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(name = "Blog")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Blog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "blog_id")
    private Long blogId;

    @NotBlank(message = "Title cannot be blank")
    @Size(min = 10, max = 200, message = "Title must be between 10 and 200 characters")
    @Column(nullable = false, columnDefinition = "NVARCHAR(200)")
    private String title;

    @Column(length = 255, unique = true)
    private String slug; // URL-friendly title (vd: "huong-dan-su-dung-tool")

    @NotBlank(message = "Content cannot be blank")
    @Column(nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String content; // HTML content

    @Size(max = 500, message = "Summary must be at most 500 characters")
    @Column(columnDefinition = "NVARCHAR(500)")
    private String summary; // Tóm tắt ngắn để hiển thị trong list

    @Column(name = "thumbnail_image", length = 500)
    private String thumbnailImage; // URL hoặc path đến ảnh thumbnail

    @Column(name = "banner_image", length = 500)
    private String bannerImage; // Ảnh banner lớn cho detail page

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @JsonIgnoreProperties({"blogs"})
    private BlogCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    @JsonIgnoreProperties({"orders", "favorites", "feedbacks", "tools", "uploadedFiles"})
    private Account author; // Manager tạo blog

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.DRAFT;

    public enum Status {
        DRAFT,      // Bản nháp
        PUBLISHED,  // Đã xuất bản
        ARCHIVED    // Lưu trữ (không hiển thị nhưng không xóa)
    }

    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;

    // Timestamps
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt; // Thời điểm publish

    @Column(name = "scheduled_publish_at")
    private LocalDateTime scheduledPublishAt; // Đặt lịch publish

    // Constructors
    public Blog() {
    }

    public Blog(String title, String content, Account author) {
        this.title = title;
        this.content = content;
        this.author = author;
        this.status = Status.DRAFT;
        this.viewCount = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.slug == null || this.slug.isEmpty()) {
            this.slug = generateSlug(this.title);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Helper methods
    private String generateSlug(String title) {
        if (title == null) return "";
        
        // Chỉ generate base slug, unique sẽ được xử lý ở service layer
        return toSlug(title);
    }

    /**
     * Public static method to convert Vietnamese text to slug
     * Used by both entity and service layer
     */
    public static String toSlug(String title) {
        if (title == null) return "";
        
        // Convert Vietnamese to ASCII
        String slug = title.toLowerCase();
        
        // Vietnamese character mappings
        slug = slug.replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a");
        slug = slug.replaceAll("[èéẹẻẽêềếệểễ]", "e");
        slug = slug.replaceAll("[ìíịỉĩ]", "i");
        slug = slug.replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o");
        slug = slug.replaceAll("[ùúụủũưừứựửữ]", "u");
        slug = slug.replaceAll("[ỳýỵỷỹ]", "y");
        slug = slug.replaceAll("đ", "d");
        
        // Remove special characters and replace spaces with hyphens
        slug = slug.replaceAll("[^a-z0-9\\s-]", "")
                   .replaceAll("\\s+", "-")
                   .replaceAll("-+", "-")
                   .trim();
        
        return slug;
    }

    public boolean isDraft() {
        return Status.DRAFT.equals(this.status);
    }

    public boolean isPublished() {
        return Status.PUBLISHED.equals(this.status);
    }

    public boolean isArchived() {
        return Status.ARCHIVED.equals(this.status);
    }

    public void publish() {
        this.status = Status.PUBLISHED;
        if (this.publishedAt == null) {
            this.publishedAt = LocalDateTime.now();
        }
    }

    public void unpublish() {
        this.status = Status.DRAFT;
    }

    public void archive() {
        this.status = Status.ARCHIVED;
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public String getAuthorName() {
        return author != null ? author.getFullName() : "Unknown";
    }

    public String getCategoryName() {
        return category != null ? category.getCategoryName() : "Uncategorized";
    }

    // Getters and Setters
    public Long getBlogId() {
        return blogId;
    }

    public void setBlogId(Long blogId) {
        this.blogId = blogId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getThumbnailImage() {
        return thumbnailImage;
    }

    public void setThumbnailImage(String thumbnailImage) {
        this.thumbnailImage = thumbnailImage;
    }

    public String getBannerImage() {
        return bannerImage;
    }

    public void setBannerImage(String bannerImage) {
        this.bannerImage = bannerImage;
    }

    public BlogCategory getCategory() {
        return category;
    }

    public void setCategory(BlogCategory category) {
        this.category = category;
    }

    public Account getAuthor() {
        return author;
    }

    public void setAuthor(Account author) {
        this.author = author;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public LocalDateTime getScheduledPublishAt() {
        return scheduledPublishAt;
    }

    public void setScheduledPublishAt(LocalDateTime scheduledPublishAt) {
        this.scheduledPublishAt = scheduledPublishAt;
    }
}
