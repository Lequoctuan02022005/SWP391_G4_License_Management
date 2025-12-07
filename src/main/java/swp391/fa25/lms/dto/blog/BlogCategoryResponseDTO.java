package swp391.fa25.lms.dto.blog;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO cho response API category với pagination (nếu cần)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlogCategoryResponseDTO {

    private List<BlogCategoryDTO> categories;
    private int totalCategories;
}
