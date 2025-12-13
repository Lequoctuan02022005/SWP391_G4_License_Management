package swp391.fa25.lms.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import swp391.fa25.lms.model.LicenseAccount;

import java.time.LocalDateTime;

public class LicenseAccountFormDTO {

    private Long licenseAccountId;

    @NotNull(message = "Vui lòng chọn License")
    private Long licenseId;

    // USER_PASSWORD
    @Size(min = 3, max = 255, message = "Username phải từ 3-255 ký tự")
    private String username;

    @Size(min = 4, max = 255, message = "Password phải từ 4-255 ký tự")
    private String password;

    // TOKEN (match đúng @Pattern trong model; null vẫn pass)
    @Pattern(
            regexp = "^[A-Za-z0-9_-]+$",
            message = "Token key chỉ được chứa chữ, số, dấu '-' hoặc '_' và không được để trống hoặc có dấu cách"
    )
    private String token;

    @NotNull(message = "Vui lòng chọn ngày bắt đầu")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startDate;

    @NotNull(message = "Vui lòng chọn ngày kết thúc")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime endDate;

    @NotNull(message = "Vui lòng chọn trạng thái")
    private LicenseAccount.Status status;

    private Boolean used = false;

    // getters/setters
    public Long getLicenseAccountId() { return licenseAccountId; }
    public void setLicenseAccountId(Long licenseAccountId) { this.licenseAccountId = licenseAccountId; }

    public Long getLicenseId() { return licenseId; }
    public void setLicenseId(Long licenseId) { this.licenseId = licenseId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

    public LicenseAccount.Status getStatus() { return status; }
    public void setStatus(LicenseAccount.Status status) { this.status = status; }

    public Boolean getUsed() { return used; }
    public void setUsed(Boolean used) { this.used = used; }
}
