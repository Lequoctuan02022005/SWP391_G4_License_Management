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

    /**
     * Xử lý sau khi thanh toán thành công:
     * - Update order status
     * - Giảm tool quantity
     * - Tạo/gán LicenseAccount
     */
    @Transactional
    public void processSuccessfulPayment(PaymentTransaction transaction) {
        List<CustomerOrder> orders = orderRepository.findByTransaction_TransactionId(transaction.getTransactionId());
        
        for (CustomerOrder order : orders) {
            // Update order status
            order.setOrderStatus(CustomerOrder.OrderStatus.SUCCESS);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
            
            // Giảm tool quantity
            Tool tool = order.getTool();
            Integer currentQty = tool.getAvailableQuantity();
            if (currentQty != null && currentQty > 0) {
                tool.setAvailableQuantity(currentQty - 1);
                toolRepository.save(tool);
            }
            
            // Tạo hoặc gán LicenseAccount
            createOrAssignLicenseAccount(order);
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
     * Tạo hoặc gán LicenseAccount cho order dựa vào loginMethod của tool
     */
    private void createOrAssignLicenseAccount(CustomerOrder order) {
        Tool tool = order.getTool();
        License license = order.getLicense();
        
        if (Tool.LoginMethod.USER_PASSWORD.equals(tool.getLoginMethod())) {
            // Tạo mới username/password
            LicenseAccount newAccount = new LicenseAccount();
            newAccount.setLicense(license);
            newAccount.setOrder(order);
            newAccount.setUsername(generateUsername(tool.getToolName()));
            newAccount.setPassword(generatePassword());
            newAccount.setStatus(LicenseAccount.Status.ACTIVE);
            newAccount.setStartDate(LocalDateTime.now());
            
            // Tính endDate dựa vào license durationDays
            if (license.getDurationDays() != null && license.getDurationDays() > 0) {
                newAccount.setEndDate(LocalDateTime.now().plusDays(license.getDurationDays()));
            }
            
            newAccount.setUsed(true);
            licenseAccountRepository.save(newAccount);
            
        } else if (Tool.LoginMethod.TOKEN.equals(tool.getLoginMethod())) {
            // Lấy token chưa dùng từ LicenseAccount của tool này
            List<LicenseAccount> unusedTokens = licenseAccountRepository
                    .findByLicense_Tool_ToolId(tool.getToolId())
                    .stream()
                    .filter(acc -> acc.getToken() != null && Boolean.FALSE.equals(acc.getUsed()))
                    .toList();
            
            if (!unusedTokens.isEmpty()) {
                LicenseAccount tokenAccount = unusedTokens.get(0);
                tokenAccount.setOrder(order);
                tokenAccount.setUsed(true);
                tokenAccount.setStartDate(LocalDateTime.now());
                
                // Tính endDate dựa vào license durationDays
                if (license.getDurationDays() != null && license.getDurationDays() > 0) {
                    tokenAccount.setEndDate(LocalDateTime.now().plusDays(license.getDurationDays()));
                }
                
                licenseAccountRepository.save(tokenAccount);
            }
        }
    }

    /**
     * Generate username duy nhất
     */
    private String generateUsername(String toolName) {
        String prefix = toolName.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
        if (prefix.length() > 10) {
            prefix = prefix.substring(0, 10);
        }
        return prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Generate password ngẫu nhiên
     */
    private String generatePassword() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}

