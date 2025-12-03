package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.LicenseAccount;

import java.util.List;

@Repository
public interface LicenseAccountRepository extends JpaRepository<LicenseAccount, Long> {

    // Check token trùng toàn hệ thống
    boolean existsByToken(String token);

    // Lấy tất cả token theo tool
    List<LicenseAccount> findByLicense_Tool_ToolId(Long toolId);

    // Lấy token cụ thể (dùng khi edit)
    LicenseAccount findByToken(String token);

    // Check token đã tồn tại trong tool (dùng trong TokenService)
    boolean existsByLicense_Tool_ToolIdAndToken(Long toolId, String token);
}