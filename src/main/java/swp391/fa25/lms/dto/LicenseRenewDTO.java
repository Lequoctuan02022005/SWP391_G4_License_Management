package swp391.fa25.lms.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LicenseRenewDTO {

    @NotNull(message = "Thiếu licenseAccountId")
    private Long licenseAccountId;

    @NotNull(message = "Vui lòng chọn ngày hết hạn mới")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime newEndDate;

    @DecimalMin(value = "0.00", message = "Số tiền phải >= 0")
    private BigDecimal amountPaid;

    // getters/setters
    public Long getLicenseAccountId() { return licenseAccountId; }
    public void setLicenseAccountId(Long licenseAccountId) { this.licenseAccountId = licenseAccountId; }

    public LocalDateTime getNewEndDate() { return newEndDate; }
    public void setNewEndDate(LocalDateTime newEndDate) { this.newEndDate = newEndDate; }

    public BigDecimal getAmountPaid() { return amountPaid; }
    public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; }
}
