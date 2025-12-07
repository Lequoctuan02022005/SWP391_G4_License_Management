package swp391.fa25.lms.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Blog_Category")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class BlogCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "blog_category_id")
    private Long blogCategoryId;

    @NotBlank(message = "Category name cannot be blank")
    @Size(max = 100, message = "Category name must be at most 100 characters")
    @Pattern(
            regexp = "^(?!.* {2,}).+$",
            message = "Category name must not contain consecutive spaces"
    )
    @Column(nullable = false, unique = true, columnDefinition = "NVARCHAR(100)")
    private String categoryName;

    @Size(max = 500, message = "Description must be at most 500 characters")
    @Pattern(
            regexp = "^(?!.* {2,}).*$",
            message = "Description must not contain consecutive spaces"
    )
    @Column(columnDefinition = "NVARCHAR(500)")
    private String description;

    @Column(length = 100)
    private String slug; // URL-friendly name (vd: "tech-news")

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL)
    @JsonIgnoreProperties({"category", "author"})
    private List<Blog> blogs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    public enum Status {
        ACTIVE,
        INACTIVE
    }

    @Column(name = "display_order")
    private Integer displayOrder = 0; // Thứ tự hiển thị

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public BlogCategory() {
    }

    public BlogCategory(String categoryName, String description, String slug) {
        this.categoryName = categoryName;
        this.description = description;
        this.slug = slug;
        this.status = Status.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.slug == null || this.slug.isEmpty()) {
            this.slug = generateSlug(this.categoryName);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Helper methods
    private String generateSlug(String name) {
        if (name == null) return "";
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim();
    }

    public boolean isActive() {
        return Status.ACTIVE.equals(this.status);
    }

    // Getters and Setters
    public Long getBlogCategoryId() {
        return blogCategoryId;
    }

    public void setBlogCategoryId(Long blogCategoryId) {
        this.blogCategoryId = blogCategoryId;
    }

    // Alias getter for DTO mapping
    public Long getCategoryId() {
        return blogCategoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public List<Blog> getBlogs() {
        return blogs;
    }

    public void setBlogs(List<Blog> blogs) {
        this.blogs = blogs;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
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
}
