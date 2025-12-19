package swp391.fa25.lms.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Bảng trung gian: Order - License
 * 1 Order có nhiều License, mỗi License có quantity
 */
@Entity
@Table(name = "Order_License")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class OrderLicense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_license_id")
    private Long orderLicenseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnoreProperties({"licenses", "licenseAccounts", "transaction"})
    private CustomerOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "license_id", nullable = false)
    @JsonIgnoreProperties({"orderLicenses", "licenseAccounts", "tool"})
    private License license;

    @Column(nullable = false)
    private Integer quantity = 1; // Số lượng LicenseAccount cần tạo cho License này trong Order này

    @Column(name = "unit_price", nullable = false)
    private Double unitPrice; // Giá đơn vị của license

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public OrderLicense() {
    }

    public OrderLicense(CustomerOrder order, License license, Integer quantity, Double unitPrice) {
        this.order = order;
        this.license = license;
        this.quantity = quantity != null && quantity > 0 ? quantity : 1;
        this.unitPrice = unitPrice;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getOrderLicenseId() {
        return orderLicenseId;
    }

    public void setOrderLicenseId(Long orderLicenseId) {
        this.orderLicenseId = orderLicenseId;
    }

    public CustomerOrder getOrder() {
        return order;
    }

    public void setOrder(CustomerOrder order) {
        this.order = order;
    }

    public License getLicense() {
        return license;
    }

    public void setLicense(License license) {
        this.license = license;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity != null && quantity > 0 ? quantity : 1;
    }

    public Double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(Double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
