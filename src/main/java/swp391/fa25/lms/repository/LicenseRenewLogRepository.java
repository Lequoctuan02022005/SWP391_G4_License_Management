package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.LicenseRenewLog;

import java.util.List;

@Repository
public interface LicenseRenewLogRepository extends JpaRepository<LicenseRenewLog, Long> {

    List<LicenseRenewLog> findByLicenseAccount_LicenseAccountIdOrderByRenewDateDesc(Long licenseAccountId);
}
