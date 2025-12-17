package swp391.fa25.lms.service;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import swp391.fa25.lms.model.CustomerOrder;
import swp391.fa25.lms.repository.CustomerOrderRepository;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class CustomerOrderService {

    private final CustomerOrderRepository orderRepo;

    public CustomerOrderService(CustomerOrderRepository orderRepo) {
        this.orderRepo = orderRepo;
    }

    private static final Map<String, String> SORT_MAP = Map.of(
            "orderId", "orderId",
            "price", "price",
            "createdAt", "createdAt",
            "toolName", "tool.toolName",
            "licenseName", "license.name",
            "orderStatus", "orderStatus"
    );

    private String normalizeSort(String sort) {
        if (!StringUtils.hasText(sort)) return "createdAt";
        return SORT_MAP.getOrDefault(sort, "createdAt");
    }

    private Sort.Direction normalizeDir(String dir) {
        return "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
    }

    public Page<CustomerOrder> getMyOrders(Long accountId,
                                           String q,
                                           CustomerOrder.OrderStatus status,
                                           LocalDateTime from,
                                           LocalDateTime to,
                                           int page, int size,
                                           String sort, String dir) {

        q = (q == null) ? "" : q.trim();

        if (from != null && to != null && from.isAfter(to)) {
            LocalDateTime tmp = from; from = to; to = tmp;
        }

        page = Math.max(page, 0);
        size = Math.min(Math.max(size, 5), 50);

        String sortProp = normalizeSort(sort);
        Sort.Direction direction = normalizeDir(dir);

        Sort sortObj = Sort.by(direction, sortProp)
                .and(Sort.by(Sort.Direction.DESC, "orderId"));

        Pageable pageable = PageRequest.of(page, size, sortObj);

        Page<CustomerOrder> rs = orderRepo.findMyOrders(accountId, q, status, from, to, pageable);

        rs.forEach(o -> o.setCanFeedbackOrReport(o.getOrderStatus() == CustomerOrder.OrderStatus.SUCCESS));
        return rs;
    }

    public CustomerOrder getMyOrderDetail(Long accountId, Long orderId) {
        CustomerOrder o = orderRepo.findByOrderIdAndAccount_AccountId(orderId, accountId)
                .orElseThrow(() -> new IllegalArgumentException("Order không tồn tại hoặc không thuộc về bạn"));
        o.setCanFeedbackOrReport(o.getOrderStatus() == CustomerOrder.OrderStatus.SUCCESS);
        return o;
    }
}
