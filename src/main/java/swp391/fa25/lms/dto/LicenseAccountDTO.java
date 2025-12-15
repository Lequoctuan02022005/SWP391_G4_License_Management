package swp391.fa25.lms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Dùng cho CUSTOMER (edit/renew) trên màn hình license_account/view.html
 */
public class LicenseAccountDTO {

    // ===== Validation groups =====
    public interface CustomerEdit {}
    public interface CustomerRenew {}

    @NotNull(message = "Thiếu licenseAccountId", groups = {CustomerEdit.class, CustomerRenew.class})
    private Long licenseAccountId;

    // --- Edit ---
    @NotBlank(message = "Username không được trống", groups = CustomerEdit.class)
    @Size(max = 100, message = "Username tối đa 100 ký tự", groups = CustomerEdit.class)
    private String username;

    @NotBlank(message = "Password không được trống", groups = CustomerEdit.class)
    @Size(max = 255, message = "Password tối đa 255 ký tự", groups = CustomerEdit.class)
    private String password;

    // --- Renew ---
    @NotNull(message = "Vui lòng chọn gói gia hạn", groups = CustomerRenew.class)
    private Long licenseId;

    // getters/setters
    public Long getLicenseAccountId() { return licenseAccountId; }
    public void setLicenseAccountId(Long licenseAccountId) { this.licenseAccountId = licenseAccountId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Long getLicenseId() { return licenseId; }
    public void setLicenseId(Long licenseId) { this.licenseId = licenseId; }
}
