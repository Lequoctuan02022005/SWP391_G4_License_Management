package swp391.fa25.lms.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Payment_Transaction")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    @JsonIgnoreProperties({"orders", "favorites", "feedbacks", "tools", "uploadedFiles"})
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TransactionType transactionType;

    public enum TransactionType {
        ORDER_PAYMENT,          // Thanh toán mua license
        LICENSE_RENEWAL,        // Thanh toán gia hạn license
        SELLER_SUBSCRIPTION     // Thanh toán gói seller
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    public enum TransactionStatus {
        PENDING,        // Đang chờ thanh toán
        PROCESSING,     // Đang xử lý (đã redirect đến VNPay)
        SUCCESS,        // Thành công
        FAILED,         // Thất bại
        CANCELLED       // Hủy bỏ
    }

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(length = 500, columnDefinition = "NVARCHAR(500)")
    private String description;

    // VNPay specific fields
    @Column(name = "vnpay_txn_ref", unique = true, length = 100)
    private String vnpayTxnRef;

    @Column(name = "vnpay_transaction_no", length = 100)
    private String vnpayTransactionNo;

    @Column(name = "vnpay_response_code", length = 10)
    private String vnpayResponseCode;

    @Column(name = "vnpay_bank_code", length = 50)
    private String vnpayBankCode;

    @Column(name = "vnpay_card_type", length = 50)
    private String vnpayCardType;

    @Column(name = "vnpay_pay_date", length = 20)
    private String vnpayPayDate;

    @Column(name = "vnpay_bank_tran_no", length = 100)
    private String vnpayBankTranNo;

    // Relationships
    @OneToMany(mappedBy = "transaction")
    @JsonIgnoreProperties({"transaction", "account", "tool", "license"})
    private List<CustomerOrder> orders;

    @OneToMany(mappedBy = "transaction")
    @JsonIgnoreProperties({"transaction", "licenseAccount"})
    private List<LicenseRenewLog> renewLogs;

    @OneToOne(mappedBy = "transaction", cascade = CascadeType.ALL)
    @JsonIgnoreProperties({"transaction"})
    private VNPayPaymentDetail vnpayDetail;

    // Timestamps
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Additional info
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    // Constructors
    public PaymentTransaction() {
    }

    public PaymentTransaction(Account account, TransactionType transactionType, BigDecimal amount, String description) {
        this.account = account;
        this.transactionType = transactionType;
        this.amount = amount;
        this.description = description;
        this.status = TransactionStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Helper methods
    public boolean isPending() {
        return TransactionStatus.PENDING.equals(this.status);
    }

    public boolean isProcessing() {
        return TransactionStatus.PROCESSING.equals(this.status);
    }

    public boolean isSuccess() {
        return TransactionStatus.SUCCESS.equals(this.status);
    }

    public boolean isFailed() {
        return TransactionStatus.FAILED.equals(this.status);
    }

    public boolean isCancelled() {
        return TransactionStatus.CANCELLED.equals(this.status);
    }

    public boolean canRetry() {
        return isFailed() || isCancelled();
    }

    public String getVnpayResponseMessage() {
        if (vnpayResponseCode == null) return "Chưa có phản hồi";
        
        return switch (vnpayResponseCode) {
            case "00" -> "Giao dịch thành công";
            case "07" -> "Trừ tiền thành công. Giao dịch bị nghi ngờ (liên quan tới lừa đảo, giao dịch bất thường)";
            case "09" -> "Thẻ/Tài khoản chưa đăng ký dịch vụ InternetBanking tại ngân hàng";
            case "10" -> "Xác thực thông tin thẻ/tài khoản không đúng quá 3 lần";
            case "11" -> "Đã hết hạn chờ thanh toán";
            case "12" -> "Thẻ/Tài khoản bị khóa";
            case "13" -> "Quý khách nhập sai mật khẩu xác thực giao dịch (OTP)";
            case "24" -> "Khách hàng hủy giao dịch";
            case "51" -> "Tài khoản không đủ số dư để thực hiện giao dịch";
            case "65" -> "Tài khoản đã vượt quá hạn mức giao dịch trong ngày";
            case "75" -> "Ngân hàng thanh toán đang bảo trì";
            case "79" -> "KH nhập sai mật khẩu thanh toán quá số lần quy định";
            default -> "Lỗi không xác định (Mã: " + vnpayResponseCode + ")";
        };
    }

    // Getters and Setters
    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVnpayTxnRef() {
        return vnpayTxnRef;
    }

    public void setVnpayTxnRef(String vnpayTxnRef) {
        this.vnpayTxnRef = vnpayTxnRef;
    }

    public String getVnpayTransactionNo() {
        return vnpayTransactionNo;
    }

    public void setVnpayTransactionNo(String vnpayTransactionNo) {
        this.vnpayTransactionNo = vnpayTransactionNo;
    }

    public String getVnpayResponseCode() {
        return vnpayResponseCode;
    }

    public void setVnpayResponseCode(String vnpayResponseCode) {
        this.vnpayResponseCode = vnpayResponseCode;
    }

    public String getVnpayBankCode() {
        return vnpayBankCode;
    }

    public void setVnpayBankCode(String vnpayBankCode) {
        this.vnpayBankCode = vnpayBankCode;
    }

    public String getVnpayCardType() {
        return vnpayCardType;
    }

    public void setVnpayCardType(String vnpayCardType) {
        this.vnpayCardType = vnpayCardType;
    }

    public String getVnpayPayDate() {
        return vnpayPayDate;
    }

    public void setVnpayPayDate(String vnpayPayDate) {
        this.vnpayPayDate = vnpayPayDate;
    }

    public String getVnpayBankTranNo() {
        return vnpayBankTranNo;
    }

    public void setVnpayBankTranNo(String vnpayBankTranNo) {
        this.vnpayBankTranNo = vnpayBankTranNo;
    }

    public List<CustomerOrder> getOrders() {
        return orders;
    }

    public void setOrders(List<CustomerOrder> orders) {
        this.orders = orders;
    }

    public List<LicenseRenewLog> getRenewLogs() {
        return renewLogs;
    }

    public void setRenewLogs(List<LicenseRenewLog> renewLogs) {
        this.renewLogs = renewLogs;
    }

    public VNPayPaymentDetail getVnpayDetail() {
        return vnpayDetail;
    }

    public void setVnpayDetail(VNPayPaymentDetail vnpayDetail) {
        this.vnpayDetail = vnpayDetail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
}
