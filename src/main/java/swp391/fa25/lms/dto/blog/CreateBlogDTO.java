package swp391.fa25.lms.dto.blog;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBlogDTO {

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(min = 10, max = 200, message = "Tiêu đề phải từ 10-200 ký tự")
    private String title;

    @Size(max = 500, message = "Tóm tắt không được quá 500 ký tự")
    private String summary;

    @NotBlank(message = "Nội dung không được để trống")
    @Size(min = 100, message = "Nội dung phải có ít nhất 100 ký tự")
    private String content;

    @NotNull(message = "Category không được để trống")
    private Long categoryId;

    private String thumbnailImage;

    private String bannerImage;

    @NotNull(message = "Trạng thái không được để trống")
    private String status; // DRAFT, PUBLISHED, ARCHIVED

    private Boolean featured = false;

    private Boolean allowComments = true;

    private LocalDateTime scheduledPublishAt;
}
