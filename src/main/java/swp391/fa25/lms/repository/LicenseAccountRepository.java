package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.LicenseAccount;

import java.util.List;

@Repository
public interface LicenseAccountRepository extends JpaRepository<LicenseAccount, Long> {
    boolean existsByToken(String token);

    LicenseAccount findByToken(String token);

    List<LicenseAccount> findByLicense_Tool_ToolId(Long toolId);

    List<LicenseAccount> findByStatusAndLicense_Tool_ToolId(LicenseAccount.Status status, Long licenseToolToolId);

}