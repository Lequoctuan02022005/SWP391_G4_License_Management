package swp391.fa25.lms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.SellerSubscription;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SellerSubscriptionRepository extends JpaRepository<SellerSubscription, Long> {

    // Lấy danh sách subscription của seller theo thứ tự mới → cũ
    List<SellerSubscription> findByAccountOrderByStartDateDesc(Account account);

    Page<SellerSubscription> findByAccountAccountId(Long accountId, Pageable pageable);
    @Query("""
        SELECT s FROM SellerSubscription s
        WHERE
            (:seller IS NULL OR LOWER(s.account.fullName) LIKE LOWER(CONCAT('%', :seller, '%')))
        AND (:packageId IS NULL OR s.sellerPackage.id = :packageId)
                 AND (
                        :packageName IS NULL OR
                        LOWER(s.sellerPackage.packageName)
                            LIKE LOWER(CONCAT('%', :packageName, '%'))
                    )
        AND (
            :status IS NULL OR
            (:status = 'ACTIVE' AND s.active = true) OR
            (:status = 'EXPIRED' AND s.active = false)
        )
        AND (:fromDate IS NULL OR s.startDate >= :fromDate)
        AND (:toDate IS NULL OR s.endDate <= :toDate)
        """)
    Page<SellerSubscription> filter(
            @Param("seller") String seller,
            @Param("packageId") Long packageId,
            @Param("packageName") String packageName,
            @Param("status") String status,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );
    @Query("""
    SELECT COALESCE(SUM(s.priceAtPurchase), 0)
    FROM SellerSubscription s
    WHERE
        (:seller IS NULL OR LOWER(s.account.fullName) LIKE LOWER(CONCAT('%', :seller, '%')))
    AND (:packageId IS NULL OR s.sellerPackage.id = :packageId)
    AND (
        :status IS NULL OR
        (:status = 'ACTIVE' AND s.active = true) OR
        (:status = 'EXPIRED' AND s.active = false)
    )
    AND (:fromDate IS NULL OR s.startDate >= :fromDate)
    AND (:toDate IS NULL OR s.endDate <= :toDate)
""")
    Long sumRevenue(
            String seller,
            Long packageId,
            String status,
            LocalDateTime fromDate,
            LocalDateTime toDate
    );
}
