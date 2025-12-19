package swp391.fa25.lms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.LicenseRenewLog;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LicenseRenewLogRepository extends JpaRepository<LicenseRenewLog, Long> {

    List<LicenseRenewLog> findByLicenseAccount_LicenseAccountIdOrderByRenewDateDesc(Long licenseAccountId);
    // ===== CUSTOMER =====
    @Query("""
        SELECT l FROM LicenseRenewLog l
        WHERE
            l.licenseAccount.order.account.accountId = :customerId
        AND (:fromDate IS NULL OR l.renewDate >= :fromDate)
        AND (:toDate IS NULL OR l.renewDate <= :toDate)
        AND (:minAmount IS NULL OR l.amountPaid >= :minAmount)
        AND (:maxAmount IS NULL OR l.amountPaid <= :maxAmount)
        ORDER BY l.renewDate DESC
    """)
    Page<LicenseRenewLog> filterForCustomer(
            @Param("customerId") Long customerId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("minAmount") Long minAmount,
            @Param("maxAmount") Long maxAmount,
            Pageable pageable
    );

    // ===== SELLER =====
    @Query("""
        SELECT l FROM LicenseRenewLog l
        WHERE
            l.licenseAccount.license.tool.seller.accountId = :sellerId
        AND (:fromDate IS NULL OR l.renewDate >= :fromDate)
        AND (:toDate IS NULL OR l.renewDate <= :toDate)
        AND (:minAmount IS NULL OR l.amountPaid >= :minAmount)
        AND (:maxAmount IS NULL OR l.amountPaid <= :maxAmount)
        ORDER BY l.renewDate DESC
    """)
    Page<LicenseRenewLog> filterForSeller(
            @Param("sellerId") Long sellerId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("minAmount") Long minAmount,
            @Param("maxAmount") Long maxAmount,
            Pageable pageable
    );
}
