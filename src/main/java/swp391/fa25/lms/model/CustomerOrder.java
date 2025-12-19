package swp391.fa25.lms.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Customer_Order")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    @JsonIgnoreProperties({
            "orders", "favorites", "feedbacks", "tools"
    })
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tool_id", nullable = false)
    @JsonIgnoreProperties({"orders", "licenses", "files", "seller", "category","feedbacks"})
    private Tool tool;

    @Column(nullable = false)
    private Double price; // Tổng giá của order (tổng của tất cả OrderLicense)

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"order", "license"})
    private List<OrderLicense> licenses; // 1 Order có nhiều License

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    private OrderStatus orderStatus;
    public enum OrderStatus { PENDING, SUCCESS, FAILED }

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;
    public enum PaymentMethod { BANK }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    @JsonIgnoreProperties({"orders", "renewLogs", "account"})
    private PaymentTransaction transaction;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Add fields để lưu trạng thái hiển thị trong View
    @Transient
    private boolean canFeedbackOrReport;

    @Transient
    private List<LicenseAccount> licenseAccounts; // Để dễ query trong view (load từ repository)

    // THÊM MỚI: Lưu txnRef unique cho retry (nullable)
    @Column(name = "last_txn_ref")
    private String lastTxnRef;

    // THÊM MỚI: updatedAt cho update callback
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public CustomerOrder() {
    }

    public CustomerOrder(Long orderId, Account account, Tool tool, Double price, OrderStatus orderStatus, PaymentMethod paymentMethod, PaymentTransaction transaction, LocalDateTime createdAt, List<OrderLicense> licenses) {
        this.orderId = orderId;
        this.account = account;
        this.tool = tool;
        this.price = price;
        this.orderStatus = orderStatus;
        this.paymentMethod = paymentMethod;
        this.transaction = transaction;
        this.createdAt = createdAt;
        this.licenses = licenses;
    }

    // Getters/Setters (thêm cho lastTxnRef)
    public String getLastTxnRef() {
        return lastTxnRef;
    }

    public void setLastTxnRef(String lastTxnRef) {
        this.lastTxnRef = lastTxnRef;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public CustomerOrder(Account account, Tool tool, Double price) {
        this.account = account;
        this.tool = tool;
        this.price = price;
        this.createdAt = LocalDateTime.now();
    }

    public boolean isCanFeedbackOrReport() {
        return canFeedbackOrReport;
    }

    public void setCanFeedbackOrReport(boolean canFeedbackOrReport) {
        this.canFeedbackOrReport = canFeedbackOrReport;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Tool getTool() {
        return tool;
    }

    public void setTool(Tool tool) {
        this.tool = tool;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public List<OrderLicense> getLicenses() {
        return licenses;
    }

    public void setLicenses(List<OrderLicense> licenses) {
        this.licenses = licenses;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }


    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentTransaction getTransaction() {
        return transaction;
    }

    public void setTransaction(PaymentTransaction transaction) {
        this.transaction = transaction;
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

    //    Thêm method helper để check retryable
    public boolean isPending() {
        return OrderStatus.PENDING.equals(orderStatus);
    }

    // Thêm tính trung bình rating của seller
    @Transient
    private double sellerRating;

    public double getSellerRating() {
        return sellerRating;
    }

    public void setSellerRating(double sellerRating) {
        this.sellerRating = sellerRating;
    }

}