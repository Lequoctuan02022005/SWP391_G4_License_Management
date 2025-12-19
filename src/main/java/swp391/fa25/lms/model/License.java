package swp391.fa25.lms.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "License")
public class License {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long licenseId;

    @NotBlank(message = "License name cannot be blank")
    @Column(nullable = false, columnDefinition = "NVARCHAR(100)")
    private String name;

    @ManyToOne
    @JoinColumn(name = "tool_id")
    @JsonIgnoreProperties({"licenses", "orders"})
    private Tool tool;

    private Integer durationDays;

    private Double price;

    @OneToMany(mappedBy = "license")
    @JsonIgnoreProperties({"license", "order"})
    private List<OrderLicense> orderLicenses; // Quan hệ với Order thông qua OrderLicense

    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "license", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"license", "order"})
    private List<LicenseAccount> licenseAccounts;

    public License() {
    }

    public License(Long licenseId, String name, Tool tool, Integer durationDays, Double price, List<OrderLicense> orderLicenses, LocalDateTime createdAt) {
        this.licenseId = licenseId;
        this.name = name;
        this.tool = tool;
        this.durationDays = durationDays;
        this.price = price;
        this.orderLicenses = orderLicenses;
        this.createdAt = createdAt;
    }


    public Long getLicenseId() {
        return licenseId;
    }

    public void setLicenseId(Long licenseId) {
        this.licenseId = licenseId;
    }

    public @NotBlank(message = "License name cannot be blank") String getName() {
        return name;
    }

    public void setName(@NotBlank(message = "License name cannot be blank") String name) {
        this.name = name;
    }

    public Tool getTool() {
        return tool;
    }

    public void setTool(Tool tool) {
        this.tool = tool;
    }

    public Integer getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(Integer durationDays) {
        this.durationDays = durationDays;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public List<OrderLicense> getOrderLicenses() {
        return orderLicenses;
    }

    public void setOrderLicenses(List<OrderLicense> orderLicenses) {
        this.orderLicenses = orderLicenses;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<LicenseAccount> getLicenseAccounts() {
        return licenseAccounts;
    }

    public void setLicenseAccounts(List<LicenseAccount> licenseAccounts) {
        this.licenseAccounts = licenseAccounts;
    }
}
