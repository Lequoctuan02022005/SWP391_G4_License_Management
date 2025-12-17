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

    @EntityGraph(attributePaths = {"tool", "license", "transaction"})
    @Query("""
    SELECT o FROM CustomerOrder o
    LEFT JOIN o.tool t
    LEFT JOIN o.license l
    LEFT JOIN o.transaction tx
    WHERE o.account.accountId = :accountId
      AND (:status IS NULL OR o.orderStatus = :status)
      AND (:from IS NULL OR o.createdAt >= :from)
      AND (:to   IS NULL OR o.createdAt <= :to)
      AND (
           :q IS NULL OR TRIM(:q) = '' OR
           LOWER(COALESCE(t.toolName, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
           LOWER(COALESCE(l.name, ''))     LIKE LOWER(CONCAT('%', :q, '%')) OR
           LOWER(COALESCE(o.lastTxnRef, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
           LOWER(COALESCE(tx.vnpayTxnRef, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
           CAST(o.orderId as string) LIKE CONCAT('%', :q, '%')
      )
""")
    Page<CustomerOrder> findMyOrders(
            @Param("accountId") Long accountId,
            @Param("q") String q,
            @Param("status") CustomerOrder.OrderStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );


    @EntityGraph(attributePaths = {"tool", "license", "transaction", "licenseAccount", "account"})
    Optional<CustomerOrder> findByOrderIdAndAccount_AccountId(Long orderId, Long accountId);
}
