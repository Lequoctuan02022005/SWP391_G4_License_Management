package swp391.fa25.lms.dto.blog;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho advanced search/filter blog
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlogSearchRequestDTO {

    private String keyword; // Search trong title, content, summary
    private Long categoryId; // Filter theo category
    private String status; // DRAFT, PUBLISHED, ARCHIVED
    private Boolean featured; // Chỉ lấy featured blogs
    private Long authorId; // Filter theo author

    // Pagination
    private Integer page = 0;
    private Integer size = 10;

    // Sort
    private String sortBy = "createdAt"; // createdAt, updatedAt, viewCount, title
    private String sortDirection = "DESC"; // ASC, DESC
}
