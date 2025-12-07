package swp391.fa25.lms.dto.blog;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO cho response API với pagination
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlogResponseDTO {

    private List<BlogListItemDTO> blogs;
    private int currentPage;
    private int totalPages;
    private long totalElements;
    private int pageSize;
    private boolean hasNext;
    private boolean hasPrevious;
}
