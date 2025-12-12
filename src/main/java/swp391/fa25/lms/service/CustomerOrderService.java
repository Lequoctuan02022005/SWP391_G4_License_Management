package swp391.fa25.lms.service;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.CustomerOrder;
import swp391.fa25.lms.repository.CustomerOrderRepository;

import java.time.LocalDateTime;

@Service
public class CustomerOrderService {

    private final CustomerOrderRepository orderRepo;

    public CustomerOrderService(CustomerOrderRepository orderRepo) {
        this.orderRepo = orderRepo;
    }

    public Page<CustomerOrder> getMyOrders(Long accountId,
                                           CustomerOrder.OrderStatus status,
                                           LocalDateTime from,
                                           LocalDateTime to,
                                           int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<CustomerOrder> rs = orderRepo.findMyOrders(accountId, status, from, to, pageable);

        // Set field hiển thị (Transient)
        rs.forEach(o -> {
            // ví dụ đơn giản: chỉ SUCCESS mới cho phép feedback/report
            o.setCanFeedbackOrReport(o.getOrderStatus() == CustomerOrder.OrderStatus.SUCCESS);
        });

        return rs;
    }

    public CustomerOrder getMyOrderDetail(Long accountId, Long orderId) {
        CustomerOrder o = orderRepo.findByOrderIdAndAccount_AccountId(orderId, accountId)
                .orElseThrow(() -> new IllegalArgumentException("Order không tồn tại hoặc không thuộc về bạn"));

        o.setCanFeedbackOrReport(o.getOrderStatus() == CustomerOrder.OrderStatus.SUCCESS);
        return o;
    }
}
