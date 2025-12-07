package swp391.fa25.lms.dto.blog;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBlogCategoryDTO {

    @NotNull(message = "Category ID không được để trống")
    private Long categoryId;

    @NotBlank(message = "Tên category không được để trống")
    @Size(min = 2, max = 100, message = "Tên category phải từ 2-100 ký tự")
    @Pattern(regexp = "^(?!\\s)(?!.*\\s{2})[\\p{L}\\p{N}\\s]+(?<!\\s)$", 
             message = "Tên category không hợp lệ (không có khoảng trắng liên tiếp)")
    private String categoryName;

    @Size(max = 500, message = "Mô tả không được quá 500 ký tự")
    private String description;

    private String slug;

    private Integer displayOrder;

    @NotNull(message = "Trạng thái không được để trống")
    @Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "Trạng thái phải là ACTIVE hoặc INACTIVE")
    private String status; // ACTIVE, INACTIVE
}
