package swp391.fa25.lms.dto.blog;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO để hiển thị thông tin BlogCategory
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlogCategoryDTO {

    private Long categoryId;
    private String categoryName;
    private String slug;
    private String description;
    private Integer displayOrder;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Computed field
    private Long blogCount; // Số lượng blog trong category

    /**
     * Constructor từ entity BlogCategory
     * Không đếm blog từ lazy-loaded collection (có thể null hoặc không load đầy đủ)
     * Sử dụng constructor với blogCount từ query thay thế
     */
    public BlogCategoryDTO(swp391.fa25.lms.model.BlogCategory category) {
        this.categoryId = category.getCategoryId();
        this.categoryName = category.getCategoryName();
        this.slug = category.getSlug();
        this.description = category.getDescription();
        this.displayOrder = category.getDisplayOrder();
        this.status = category.getStatus() != null ? category.getStatus().name() : null;
        this.createdAt = category.getCreatedAt();
        this.updatedAt = category.getUpdatedAt();
        
        // Không đếm blog từ lazy collection - để service layer xử lý bằng query
        this.blogCount = 0L;
    }

    /**
     * Constructor với blog count từ query
     */
    public BlogCategoryDTO(swp391.fa25.lms.model.BlogCategory category, Long blogCount) {
        this(category);
        this.blogCount = blogCount;
    }

    public boolean isActive() {
        return "ACTIVE".equals(this.status);
    }
}
