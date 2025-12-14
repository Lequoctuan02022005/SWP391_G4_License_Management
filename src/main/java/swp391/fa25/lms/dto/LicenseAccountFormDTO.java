package swp391.fa25.lms.dto;

import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import swp391.fa25.lms.model.LicenseAccount;

import java.time.LocalDateTime;

/**
 * Dùng cho ADMIN create/edit LicenseAccount (nếu có).
 * Lưu ý: tùy Tool.loginMethod (TOKEN / USER_PASSWORD) mà chỉ 1 nhóm field được dùng.
 */
public class LicenseAccountFormDTO {

    @NotNull(message = "Vui lòng chọn gói license")
    private Long licenseId;

    @NotNull(message = "Thiếu ngày bắt đầu")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startDate;

    @NotNull(message = "Thiếu ngày kết thúc")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime endDate;

    @NotNull(message = "Thiếu trạng thái")
    private LicenseAccount.Status status;

    private Boolean used;

    // credentials
    private String token;
    private String username;
    private String password;

    // getters/setters
    public Long getLicenseId() { return licenseId; }
    public void setLicenseId(Long licenseId) { this.licenseId = licenseId; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

    public LicenseAccount.Status getStatus() { return status; }
    public void setStatus(LicenseAccount.Status status) { this.status = status; }

    public Boolean getUsed() { return used; }
    public void setUsed(Boolean used) { this.used = used; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
