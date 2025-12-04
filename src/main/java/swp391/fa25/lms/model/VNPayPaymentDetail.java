package swp391.fa25.lms.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "VNPay_Payment_Detail")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class VNPayPaymentDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "detail_id")
    private Long detailId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", unique = true, nullable = false)
    @JsonIgnoreProperties({"vnpayDetail", "orders", "renewLogs", "account"})
    private PaymentTransaction transaction;

    // ========== VNPay Request Parameters ==========
    
    @Column(name = "vnp_version", length = 10)
    private String vnpVersion = "2.1.0";

    @Column(name = "vnp_command", length = 20)
    private String vnpCommand = "pay";

    @Column(name = "vnp_tmn_code", length = 20)
    private String vnpTmnCode;

    @Column(name = "vnp_amount")
    private Long vnpAmount;

    @Column(name = "vnp_currency_code", length = 5)
    private String vnpCurrCode = "VND";

    @Column(name = "vnp_txn_ref", length = 100)
    private String vnpTxnRef;

    @Column(name = "vnp_order_info", length = 255, columnDefinition = "NVARCHAR(255)")
    private String vnpOrderInfo;

    @Column(name = "vnp_order_type", length = 20)
    private String vnpOrderType;

    @Column(name = "vnp_locale", length = 5)
    private String vnpLocale = "vn";

    @Column(name = "vnp_return_url", length = 500)
    private String vnpReturnUrl;

    @Column(name = "vnp_ip_addr", length = 50)
    private String vnpIpAddr;

    @Column(name = "vnp_create_date", length = 20)
    private String vnpCreateDate;

    @Column(name = "vnp_expire_date", length = 20)
    private String vnpExpireDate;

    // Customer billing info (optional)
    @Column(name = "vnp_bill_mobile", length = 20)
    private String vnpBillMobile;

    @Column(name = "vnp_bill_email", length = 100)
    private String vnpBillEmail;

    @Column(name = "vnp_bill_first_name", length = 100, columnDefinition = "NVARCHAR(100)")
    private String vnpBillFirstName;

    @Column(name = "vnp_bill_last_name", length = 100, columnDefinition = "NVARCHAR(100)")
    private String vnpBillLastName;

    @Column(name = "vnp_bill_address", length = 200, columnDefinition = "NVARCHAR(200)")
    private String vnpBillAddress;

    @Column(name = "vnp_bill_city", length = 100, columnDefinition = "NVARCHAR(100)")
    private String vnpBillCity;

    @Column(name = "vnp_bill_country", length = 100, columnDefinition = "NVARCHAR(100)")
    private String vnpBillCountry;

    @Column(name = "vnp_bill_state", length = 100, columnDefinition = "NVARCHAR(100)")
    private String vnpBillState;

    // Bank selection
    @Column(name = "vnp_bank_code", length = 50)
    private String vnpBankCode;

    // ========== VNPay Response Parameters ==========
    
    @Column(name = "vnp_transaction_no", length = 100)
    private String vnpTransactionNo;

    @Column(name = "vnp_transaction_status", length = 10)
    private String vnpTransactionStatus;

    @Column(name = "vnp_response_code", length = 10)
    private String vnpResponseCode;

    @Column(name = "vnp_bank_tran_no", length = 100)
    private String vnpBankTranNo;

    @Column(name = "vnp_card_type", length = 50)
    private String vnpCardType;

    @Column(name = "vnp_pay_date", length = 20)
    private String vnpPayDate;

    @Column(name = "vnp_secure_hash", length = 500)
    private String vnpSecureHash;

    @Column(name = "vnp_secure_hash_type", length = 20)
    private String vnpSecureHashType = "SHA256";

    // ========== Additional Info ==========
    
    @Column(name = "payment_url", length = 1000)
    private String paymentUrl;

    @Column(name = "raw_request_params", columnDefinition = "TEXT")
    private String rawRequestParams;

    @Column(name = "raw_response_params", columnDefinition = "TEXT")
    private String rawResponseParams;

    @Column(name = "callback_received")
    private Boolean callbackReceived = false;

    @Column(name = "callback_at")
    private LocalDateTime callbackAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public VNPayPaymentDetail() {
    }

    public VNPayPaymentDetail(PaymentTransaction transaction) {
        this.transaction = transaction;
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
    public boolean isCallbackReceived() {
        return Boolean.TRUE.equals(callbackReceived);
    }

    public void markCallbackReceived() {
        this.callbackReceived = true;
        this.callbackAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getDetailId() {
        return detailId;
    }

    public void setDetailId(Long detailId) {
        this.detailId = detailId;
    }

    public PaymentTransaction getTransaction() {
        return transaction;
    }

    public void setTransaction(PaymentTransaction transaction) {
        this.transaction = transaction;
    }

    public String getVnpVersion() {
        return vnpVersion;
    }

    public void setVnpVersion(String vnpVersion) {
        this.vnpVersion = vnpVersion;
    }

    public String getVnpCommand() {
        return vnpCommand;
    }

    public void setVnpCommand(String vnpCommand) {
        this.vnpCommand = vnpCommand;
    }

    public String getVnpTmnCode() {
        return vnpTmnCode;
    }

    public void setVnpTmnCode(String vnpTmnCode) {
        this.vnpTmnCode = vnpTmnCode;
    }

    public Long getVnpAmount() {
        return vnpAmount;
    }

    public void setVnpAmount(Long vnpAmount) {
        this.vnpAmount = vnpAmount;
    }

    public String getVnpCurrCode() {
        return vnpCurrCode;
    }

    public void setVnpCurrCode(String vnpCurrCode) {
        this.vnpCurrCode = vnpCurrCode;
    }

    public String getVnpTxnRef() {
        return vnpTxnRef;
    }

    public void setVnpTxnRef(String vnpTxnRef) {
        this.vnpTxnRef = vnpTxnRef;
    }

    public String getVnpOrderInfo() {
        return vnpOrderInfo;
    }

    public void setVnpOrderInfo(String vnpOrderInfo) {
        this.vnpOrderInfo = vnpOrderInfo;
    }

    public String getVnpOrderType() {
        return vnpOrderType;
    }

    public void setVnpOrderType(String vnpOrderType) {
        this.vnpOrderType = vnpOrderType;
    }

    public String getVnpLocale() {
        return vnpLocale;
    }

    public void setVnpLocale(String vnpLocale) {
        this.vnpLocale = vnpLocale;
    }

    public String getVnpReturnUrl() {
        return vnpReturnUrl;
    }

    public void setVnpReturnUrl(String vnpReturnUrl) {
        this.vnpReturnUrl = vnpReturnUrl;
    }

    public String getVnpIpAddr() {
        return vnpIpAddr;
    }

    public void setVnpIpAddr(String vnpIpAddr) {
        this.vnpIpAddr = vnpIpAddr;
    }

    public String getVnpCreateDate() {
        return vnpCreateDate;
    }

    public void setVnpCreateDate(String vnpCreateDate) {
        this.vnpCreateDate = vnpCreateDate;
    }

    public String getVnpExpireDate() {
        return vnpExpireDate;
    }

    public void setVnpExpireDate(String vnpExpireDate) {
        this.vnpExpireDate = vnpExpireDate;
    }

    public String getVnpBillMobile() {
        return vnpBillMobile;
    }

    public void setVnpBillMobile(String vnpBillMobile) {
        this.vnpBillMobile = vnpBillMobile;
    }

    public String getVnpBillEmail() {
        return vnpBillEmail;
    }

    public void setVnpBillEmail(String vnpBillEmail) {
        this.vnpBillEmail = vnpBillEmail;
    }

    public String getVnpBillFirstName() {
        return vnpBillFirstName;
    }

    public void setVnpBillFirstName(String vnpBillFirstName) {
        this.vnpBillFirstName = vnpBillFirstName;
    }

    public String getVnpBillLastName() {
        return vnpBillLastName;
    }

    public void setVnpBillLastName(String vnpBillLastName) {
        this.vnpBillLastName = vnpBillLastName;
    }

    public String getVnpBillAddress() {
        return vnpBillAddress;
    }

    public void setVnpBillAddress(String vnpBillAddress) {
        this.vnpBillAddress = vnpBillAddress;
    }

    public String getVnpBillCity() {
        return vnpBillCity;
    }

    public void setVnpBillCity(String vnpBillCity) {
        this.vnpBillCity = vnpBillCity;
    }

    public String getVnpBillCountry() {
        return vnpBillCountry;
    }

    public void setVnpBillCountry(String vnpBillCountry) {
        this.vnpBillCountry = vnpBillCountry;
    }

    public String getVnpBillState() {
        return vnpBillState;
    }

    public void setVnpBillState(String vnpBillState) {
        this.vnpBillState = vnpBillState;
    }

    public String getVnpBankCode() {
        return vnpBankCode;
    }

    public void setVnpBankCode(String vnpBankCode) {
        this.vnpBankCode = vnpBankCode;
    }

    public String getVnpTransactionNo() {
        return vnpTransactionNo;
    }

    public void setVnpTransactionNo(String vnpTransactionNo) {
        this.vnpTransactionNo = vnpTransactionNo;
    }

    public String getVnpTransactionStatus() {
        return vnpTransactionStatus;
    }

    public void setVnpTransactionStatus(String vnpTransactionStatus) {
        this.vnpTransactionStatus = vnpTransactionStatus;
    }

    public String getVnpResponseCode() {
        return vnpResponseCode;
    }

    public void setVnpResponseCode(String vnpResponseCode) {
        this.vnpResponseCode = vnpResponseCode;
    }

    public String getVnpBankTranNo() {
        return vnpBankTranNo;
    }

    public void setVnpBankTranNo(String vnpBankTranNo) {
        this.vnpBankTranNo = vnpBankTranNo;
    }

    public String getVnpCardType() {
        return vnpCardType;
    }

    public void setVnpCardType(String vnpCardType) {
        this.vnpCardType = vnpCardType;
    }

    public String getVnpPayDate() {
        return vnpPayDate;
    }

    public void setVnpPayDate(String vnpPayDate) {
        this.vnpPayDate = vnpPayDate;
    }

    public String getVnpSecureHash() {
        return vnpSecureHash;
    }

    public void setVnpSecureHash(String vnpSecureHash) {
        this.vnpSecureHash = vnpSecureHash;
    }

    public String getVnpSecureHashType() {
        return vnpSecureHashType;
    }

    public void setVnpSecureHashType(String vnpSecureHashType) {
        this.vnpSecureHashType = vnpSecureHashType;
    }

    public String getPaymentUrl() {
        return paymentUrl;
    }

    public void setPaymentUrl(String paymentUrl) {
        this.paymentUrl = paymentUrl;
    }

    public String getRawRequestParams() {
        return rawRequestParams;
    }

    public void setRawRequestParams(String rawRequestParams) {
        this.rawRequestParams = rawRequestParams;
    }

    public String getRawResponseParams() {
        return rawResponseParams;
    }

    public void setRawResponseParams(String rawResponseParams) {
        this.rawResponseParams = rawResponseParams;
    }

    public Boolean getCallbackReceived() {
        return callbackReceived;
    }

    public void setCallbackReceived(Boolean callbackReceived) {
        this.callbackReceived = callbackReceived;
    }

    public LocalDateTime getCallbackAt() {
        return callbackAt;
    }

    public void setCallbackAt(LocalDateTime callbackAt) {
        this.callbackAt = callbackAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
