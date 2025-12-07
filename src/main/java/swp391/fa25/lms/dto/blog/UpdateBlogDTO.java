package swp391.fa25.lms.dto.blog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBlogDTO {

    @NotNull(message = "Blog ID không được để trống")
    private Long blogId;

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(min = 10, max = 200, message = "Tiêu đề phải từ 10-200 ký tự")
    private String title;

    private String slug; // Optional

    @Size(max = 500, message = "Tóm tắt không được quá 500 ký tự")
    private String summary;

    @Size(max = 500, message = "Mô tả ngắn không được quá 500 ký tự")
    private String excerpt; // Alias for summary

    @NotBlank(message = "Nội dung không được để trống")
    @Size(min = 100, message = "Nội dung phải có ít nhất 100 ký tự")
    private String content;

    @NotNull(message = "Category không được để trống")
    private Long categoryId;

    private String thumbnailImage;

    private String bannerImage;

    @NotNull(message = "Trạng thái không được để trống")
    @Pattern(regexp = "^(DRAFT|PUBLISHED|ARCHIVED)$", 
             message = "Trạng thái phải là DRAFT, PUBLISHED hoặc ARCHIVED")
    private String status; // DRAFT, PUBLISHED, ARCHIVED

    private LocalDateTime scheduledPublishAt;

    // Getters for aliases
    public String getExcerpt() {
        return excerpt != null ? excerpt : summary;
    }

    public void setExcerpt(String excerpt) {
        this.excerpt = excerpt;
        if (this.summary == null) {
            this.summary = excerpt;
        }
    }
}
