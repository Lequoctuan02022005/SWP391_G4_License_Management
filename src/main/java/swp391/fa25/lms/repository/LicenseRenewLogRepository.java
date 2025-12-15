package swp391.fa25.lms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.LicenseRenewLog;

import java.util.List;

@Repository
public interface LicenseRenewLogRepository extends JpaRepository<LicenseRenewLog, Long> {

    List<LicenseRenewLog> findByLicenseAccount_LicenseAccountIdOrderByRenewDateDesc(Long licenseAccountId);

    @Query("""
        SELECT l FROM LicenseRenewLog l
        WHERE l.licenseAccount.order.account.accountId = :accountId
        ORDER BY l.renewDate DESC
    """)
    Page<LicenseRenewLog> findByCustomer(
            @Param("accountId") Long accountId,
            Pageable pageable
    );
}
