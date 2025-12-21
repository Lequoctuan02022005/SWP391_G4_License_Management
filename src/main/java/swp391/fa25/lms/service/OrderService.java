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
    private final OrderLicenseRepository orderLicenseRepository;

    @Autowired
    private ToolRepository toolRepository;

    /**
     * ✅ Mỗi Order có nhiều License thông qua OrderLicense
     */
    @Transactional
    public List<CustomerOrder> createOrdersFromCartItems(Account account,
                                                         List<CartItem> items,
                                                         PaymentTransaction transaction) {
        List<CustomerOrder> orders = new ArrayList<>();
        if (items == null || items.isEmpty()) return orders;

        for (CartItem item : items) {
            // Tạo Order
            CustomerOrder order = new CustomerOrder();
            order.setAccount(account);
            order.setTool(item.getTool());
            order.setPrice(item.getTotalPrice()); // Tổng giá = unitPrice * quantity
            order.setOrderStatus(CustomerOrder.OrderStatus.PENDING);
            order.setPaymentMethod(CustomerOrder.PaymentMethod.BANK);
            order.setTransaction(transaction);
            order.setCreatedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());
            order = orderRepository.save(order);

            // Tạo OrderLicense (1 Order có 1 License với quantity)
            OrderLicense orderLicense = new OrderLicense();
            orderLicense.setOrder(order);
            orderLicense.setLicense(item.getLicense());
            orderLicense.setQuantity(item.getQuantity() != null ? item.getQuantity() : 1);
            orderLicense.setUnitPrice(item.getUnitPrice() != null ? item.getUnitPrice() : item.getLicense().getPrice());
            orderLicenseRepository.save(orderLicense);

            orders.add(order);
        }
        return orders;
    }

    /**
     * ✅ Tạo 1 Order với 1 License (thông qua OrderLicense) có quantity = qty
     */
    @Transactional
    public List<CustomerOrder> createOrderFromBuyNow(Account account,
                                                     Tool tool,
                                                     License license,
                                                     int qty,
                                                     PaymentTransaction transaction) {
        if (qty <= 0) qty = 1;

        List<CustomerOrder> orders = new ArrayList<>();

        // Tạo Order
            CustomerOrder order = new CustomerOrder();
            order.setAccount(account);
            order.setTool(tool);
        order.setPrice(license.getPrice() != null ? license.getPrice() * qty : 0.0); // Tổng giá = unitPrice * quantity
            order.setOrderStatus(CustomerOrder.OrderStatus.PENDING);
            order.setPaymentMethod(CustomerOrder.PaymentMethod.BANK);
            order.setTransaction(transaction);
            order.setCreatedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());
        order = orderRepository.save(order);

        // Tạo OrderLicense
        OrderLicense orderLicense = new OrderLicense();
        orderLicense.setOrder(order);
        orderLicense.setLicense(license);
        orderLicense.setQuantity(qty);
        orderLicense.setUnitPrice(license.getPrice());
        orderLicenseRepository.save(orderLicense);

        orders.add(order);

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

            // 2) Giảm tool quantity và cấp LicenseAccount cho từng OrderLicense
            Tool tool = order.getTool();
            List<OrderLicense> orderLicenses = orderLicenseRepository.findByOrder_OrderId(order.getOrderId());
            
            for (OrderLicense orderLicense : orderLicenses) {
                // Giảm tool quantity
            if (tool != null) {
                Integer currentQty = tool.getAvailableQuantity();
                    Integer licenseQty = orderLicense.getQuantity() != null ? orderLicense.getQuantity() : 1;
                if (currentQty != null && currentQty > 0) {
                        tool.setAvailableQuantity(Math.max(0, currentQty - licenseQty));
                    toolRepository.save(tool);
                }
            }

                // 3) ✅ Cấp LicenseAccount thông qua License (theo quantity của OrderLicense)
                provisionLicenseAccounts(orderLicense);
            }
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
    private void provisionLicenseAccounts(OrderLicense orderLicense) {
        if (orderLicense == null) return;

        CustomerOrder order = orderLicense.getOrder();
        License license = orderLicense.getLicense();
        Tool tool = order != null ? order.getTool() : null;
        
        if (tool == null || license == null) return;

        Integer quantity = orderLicense.getQuantity() != null ? orderLicense.getQuantity() : 1;

        if (Tool.LoginMethod.USER_PASSWORD.equals(tool.getLoginMethod())) {
            // TH1: Tạo mới username/password cho mỗi account
            for (int i = 0; i < quantity; i++) {
            LicenseAccount la = new LicenseAccount();
                la.setLicense(license); // ✅ Gán qua License (không trực tiếp từ Order)
                la.setOrder(order); // Tracking: biết account này thuộc order nào

            la.setUsername(generateUsername(tool.getToolName()));
            la.setPassword(generatePassword());

            // ✅ CHƯA kích hoạt
            la.setStatus(LicenseAccount.Status.ACTIVE);
            la.setUsed(false);
            la.setStartDate(null);
            la.setEndDate(null);

                licenseAccountRepository.save(la);
            }

        } else if (Tool.LoginMethod.TOKEN.equals(tool.getLoginMethod())) {
            // TH2: Lấy token từ License (tool) theo số lượng quantity
            List<LicenseAccount> unusedTokens = licenseAccountRepository
                    .findByLicense_Tool_ToolId(tool.getToolId())
                    .stream()
                    .filter(acc -> acc.getToken() != null)
                    .filter(acc -> Boolean.FALSE.equals(acc.getUsed()))
                    .filter(acc -> acc.getOrder() == null) // ✅ chưa bị reserve
                    .limit(quantity) // Lấy đúng số lượng cần
                    .toList();

            if (unusedTokens.size() < quantity) {
                // Không đủ token, có thể throw exception hoặc log warning
                throw new IllegalStateException(
                    String.format("Không đủ token cho order. Cần %d token nhưng chỉ có %d token available", 
                        quantity, unusedTokens.size())
                );
            }

            for (LicenseAccount tokenAcc : unusedTokens) {
                // reserve token cho order
                tokenAcc.setOrder(order); // Tracking: biết token này thuộc order nào

                // ✅ đảm bảo license đúng với đơn mua (để durationDays đúng)
                tokenAcc.setLicense(license);

                // ✅ CHƯA kích hoạt
                tokenAcc.setStatus(LicenseAccount.Status.ACTIVE);
                tokenAcc.setUsed(false);
                tokenAcc.setStartDate(null);
                tokenAcc.setEndDate(null);

                licenseAccountRepository.save(tokenAcc);
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
