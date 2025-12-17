package swp391.fa25.lms.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.fa25.lms.model.*;
import swp391.fa25.lms.repository.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final LicenseAccountRepository licenseAccountRepository;

    @Autowired
    private ToolRepository toolRepository;

    /**
     * Tạo danh sách CustomerOrder từ CartItems và link với PaymentTransaction
     */
    @Transactional
    public List<CustomerOrder> createOrdersFromCart(Cart cart, PaymentTransaction transaction) {
        List<CustomerOrder> orders = new ArrayList<>();

        for (CartItem item : cart.getItems()) {
            CustomerOrder order = new CustomerOrder();
            order.setAccount(cart.getAccount());
            order.setTool(item.getTool());
            order.setLicense(item.getLicense());
            order.setPrice(item.getTotalPrice());
            order.setOrderStatus(CustomerOrder.OrderStatus.PENDING);
            order.setPaymentMethod(CustomerOrder.PaymentMethod.BANK);
            order.setTransaction(transaction);
            order.setCreatedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());

            orders.add(orderRepository.save(order));
        }

        return orders;
    }

    @Transactional
    public List<CustomerOrder> createOrdersFromCartItems(Account account,
                                                         List<CartItem> items,
                                                         PaymentTransaction transaction) {
        List<CustomerOrder> orders = new ArrayList<>();
        if (items == null || items.isEmpty()) return orders;

        for (CartItem item : items) {
            CustomerOrder order = new CustomerOrder();
            order.setAccount(account);
            order.setTool(item.getTool());
            order.setLicense(item.getLicense());
            order.setPrice(item.getTotalPrice());
            order.setOrderStatus(CustomerOrder.OrderStatus.PENDING);
            order.setPaymentMethod(CustomerOrder.PaymentMethod.BANK);
            order.setTransaction(transaction);
            order.setCreatedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());

            orders.add(orderRepository.save(order));
        }
        return orders;
    }

    @Transactional
    public List<CustomerOrder> createOrderFromBuyNow(Account account,
                                                     Tool tool,
                                                     License license,
                                                     int qty,
                                                     PaymentTransaction transaction) {
        if (qty <= 0) qty = 1;

        List<CustomerOrder> orders = new ArrayList<>();

        for (int i = 0; i < qty; i++) {
            CustomerOrder order = new CustomerOrder();
            order.setAccount(account);
            order.setTool(tool);
            order.setLicense(license);
            order.setPrice(license.getPrice());
            order.setOrderStatus(CustomerOrder.OrderStatus.PENDING);
            order.setPaymentMethod(CustomerOrder.PaymentMethod.BANK);
            order.setTransaction(transaction);
            order.setCreatedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());

            orders.add(orderRepository.save(order));
        }

        return orders;
    }

    /**
     * ✅ SUCCESS PAYMENT:
     * - order -> SUCCESS
     * - giảm quantity
     * - cấp LicenseAccount nhưng CHƯA kích hoạt, CHƯA tính hạn
     */
    @Transactional
    public void processSuccessfulPayment(PaymentTransaction transaction) {
        List<CustomerOrder> orders = orderRepository.findByTransaction_TransactionId(transaction.getTransactionId());

        for (CustomerOrder order : orders) {

            // 1) Update order status
            order.setOrderStatus(CustomerOrder.OrderStatus.SUCCESS);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);

            // 2) Giảm tool quantity
            Tool tool = order.getTool();
            if (tool != null) {
                Integer currentQty = tool.getAvailableQuantity();
                if (currentQty != null && currentQty > 0) {
                    tool.setAvailableQuantity(currentQty - 1);
                    toolRepository.save(tool);
                }
            }

            // 3) ✅ Cấp LicenseAccount nhưng chưa kích hoạt
            provisionLicenseAccount(order);
        }
    }

    /**
     * Xử lý khi thanh toán thất bại
     */
    @Transactional
    public void processFailedPayment(PaymentTransaction transaction) {
        List<CustomerOrder> orders = orderRepository.findByTransaction_TransactionId(transaction.getTransactionId());

        for (CustomerOrder order : orders) {
            order.setOrderStatus(CustomerOrder.OrderStatus.FAILED);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
        }
    }

    /**
     * ✅ CẤP LicenseAccount cho order theo loginMethod
     * - KHÔNG set startDate/endDate
     * - used = false (chưa kích hoạt)
     * - status giữ ACTIVE (để UI “Dùng dịch vụ” vẫn hiện), nhưng “kích hoạt” sẽ dựa theo used/startDate.
     *
     * Lưu ý TOKEN:
     * - Không dùng used để “reserve”, mà reserve bằng cách gán Order (order != null)
     * - Khi chọn token thì lọc acc.getOrder() == null để tránh cấp trùng token
     */
    private void provisionLicenseAccount(CustomerOrder order) {
        if (order == null) return;

        Tool tool = order.getTool();
        License license = order.getLicense();
        if (tool == null || license == null) return;

        // Nếu order đã có licenseAccount rồi thì thôi (idempotent)
        if (order.getLicenseAccount() != null) return;

        if (Tool.LoginMethod.USER_PASSWORD.equals(tool.getLoginMethod())) {

            LicenseAccount la = new LicenseAccount();
            la.setLicense(license);
            la.setOrder(order);

            la.setUsername(generateUsername(tool.getToolName()));
            la.setPassword(generatePassword());

            // ✅ CHƯA kích hoạt
            la.setStatus(LicenseAccount.Status.ACTIVE);
            la.setUsed(false);
            la.setStartDate(null);
            la.setEndDate(null);

            la = licenseAccountRepository.save(la);

            // ✅ gắn ngược lại order để view order.getLicenseAccount() luôn có
            order.setLicenseAccount(la);
            orderRepository.save(order);

        } else if (Tool.LoginMethod.TOKEN.equals(tool.getLoginMethod())) {

            List<LicenseAccount> unusedTokens = licenseAccountRepository
                    .findByLicense_Tool_ToolId(tool.getToolId())
                    .stream()
                    .filter(acc -> acc.getToken() != null)
                    .filter(acc -> Boolean.FALSE.equals(acc.getUsed()))
                    .filter(acc -> acc.getOrder() == null) // ✅ chưa bị reserve
                    .toList();

            if (!unusedTokens.isEmpty()) {
                LicenseAccount tokenAcc = unusedTokens.get(0);

                // reserve token cho order
                tokenAcc.setOrder(order);

                // ✅ đảm bảo license đúng với đơn mua (để durationDays đúng)
                tokenAcc.setLicense(license);

                // ✅ CHƯA kích hoạt
                tokenAcc.setStatus(LicenseAccount.Status.ACTIVE);
                tokenAcc.setUsed(false);
                tokenAcc.setStartDate(null);
                tokenAcc.setEndDate(null);

                tokenAcc = licenseAccountRepository.save(tokenAcc);

                order.setLicenseAccount(tokenAcc);
                orderRepository.save(order);
            }
        }
    }

    /**
     * Generate username duy nhất
     */
    private String generateUsername(String toolName) {
        String prefix = (toolName == null ? "tool" : toolName).replaceAll("[^A-Za-z0-9]", "").toLowerCase();
        if (prefix.length() > 10) prefix = prefix.substring(0, 10);
        return prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Generate password ngẫu nhiên
     */
    private String generatePassword() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
