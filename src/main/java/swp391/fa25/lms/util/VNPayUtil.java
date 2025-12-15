package swp391.fa25.lms.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class VNPayUtil {

    @Value("${vnpay.tmnCode}")
    private String tmnCode;

    @Value("${vnpay.hashSecret}")
    private String hashSecret;

    @Value("${vnpay.baseUrl}")
    private String baseUrl;

    /**
     * Tạo URL thanh toán VNPay
     * 
     * @param amount Số tiền (VND)
     * @param orderInfo Thông tin đơn hàng (format: "SELLER_{packageId}_{accountId}" hoặc orderId)
     * @param txnRef Mã giao dịch (unique)
     * @param returnUrl URL callback
     * @param request HttpServletRequest
     * @return VNPay payment URL
     */
    public String createPaymentUrl(long amount, String orderInfo, String txnRef, 
                                   String returnUrl, HttpServletRequest request) {
        try {
            Map<String, String> vnpParams = new LinkedHashMap<>();
            vnpParams.put("vnp_Version", "2.1.0");
            vnpParams.put("vnp_Command", "pay");
            vnpParams.put("vnp_TmnCode", tmnCode.trim());
            vnpParams.put("vnp_Amount", String.valueOf(amount * 100));
            vnpParams.put("vnp_CurrCode", "VND");
            vnpParams.put("vnp_TxnRef", txnRef);
            vnpParams.put("vnp_OrderInfo", orderInfo);
            vnpParams.put("vnp_OrderType", "billpayment");
            vnpParams.put("vnp_Locale", "vn");
            vnpParams.put("vnp_ReturnUrl", returnUrl.trim());
            vnpParams.put("vnp_IpAddr", getIpAddress(request));
            vnpParams.put("vnp_CreateDate", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
            
            return buildPaymentUrl(vnpParams);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Verify hash từ VNPay callback
     */
    public boolean verifyHash(Map<String, String> params) {
        try {
            String vnpSecureHash = params.get("vnp_SecureHash");
            if (vnpSecureHash == null) {
                return false;
            }

            Map<String, String> clonedParams = new HashMap<>(params);
            clonedParams.remove("vnp_SecureHash");
            clonedParams.remove("vnp_SecureHashType");

            List<String> fieldNames = new ArrayList<>(clonedParams.keySet());
            Collections.sort(fieldNames);

            StringBuilder hashData = new StringBuilder();
            for (String fieldName : fieldNames) {
                String fieldValue = clonedParams.get(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    if (hashData.length() > 0) {
                        hashData.append('&');
                    }
                    hashData.append(fieldName).append('=')
                            .append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));
                }
            }

            String calculatedHash = hmacSHA512(hashSecret.trim(), hashData.toString());
            return calculatedHash.equalsIgnoreCase(vnpSecureHash);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Tạo unique transaction reference
     */
    public String generateTxnRef() {
        return String.valueOf(System.currentTimeMillis());
    }

    /**
     * Lấy IP address từ request
     */
    public String getIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }

    // ================ PRIVATE HELPERS ================

    /**
     * Build VNPay payment URL với secure hash
     */
    private String buildPaymentUrl(Map<String, String> params) {
        try {
            List<String> fieldNames = new ArrayList<>(params.keySet());
            Collections.sort(fieldNames);

            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();

            for (String fieldName : fieldNames) {
                String value = params.get(fieldName);
                if (value == null || value.isEmpty()) continue;

                if (hashData.length() > 0) {
                    hashData.append('&');
                    query.append('&');
                }

                String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8);
                String encodedField = URLEncoder.encode(fieldName, StandardCharsets.UTF_8);

                // HashData: field=encodedValue (không encode field name)
                hashData.append(fieldName).append('=').append(encodedValue);
                
                // Query: encodedField=encodedValue (encode cả 2)
                query.append(encodedField).append('=').append(encodedValue);
            }

            String secureHash = hmacSHA512(hashSecret.trim(), hashData.toString()).toUpperCase(Locale.ROOT);
            return baseUrl + "?" + query + "&vnp_SecureHash=" + secureHash;

        } catch (Exception e) {
            throw new RuntimeException("Error building VNPay URL", e);
        }
    }

    /**
     * HMAC SHA512 hashing
     */
    private String hmacSHA512(String key, String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac.init(secretKey);
            byte[] hash = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
            
        } catch (Exception e) {
            throw new RuntimeException("Error generating HMAC SHA512", e);
        }
    }
}

