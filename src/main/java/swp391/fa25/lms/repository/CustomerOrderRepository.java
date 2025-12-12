package swp391.fa25.lms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.CustomerOrder;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    @Query("""
        SELECT o FROM CustomerOrder o
        WHERE o.account.accountId = :accountId
          AND (:status IS NULL OR o.orderStatus = :status)
          AND (:from IS NULL OR o.createdAt >= :from)
          AND (:to IS NULL OR o.createdAt <= :to)
        ORDER BY o.createdAt DESC
    """)
    Page<CustomerOrder> findMyOrders(
            @Param("accountId") Long accountId,
            @Param("status") CustomerOrder.OrderStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    // Detail (có ownership check)
    @EntityGraph(attributePaths = {"tool", "license", "transaction", "licenseAccount"})
    Optional<CustomerOrder> findByOrderIdAndAccount_AccountId(Long orderId, Long accountId);
}
