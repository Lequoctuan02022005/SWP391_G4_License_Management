package swp391.fa25.lms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import swp391.fa25.lms.model.LicenseAccount;
import swp391.fa25.lms.model.Tool;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LicenseAccountRepository extends JpaRepository<LicenseAccount, Long> {

    boolean existsByToken(String token);

    LicenseAccount findByToken(String token);

    List<LicenseAccount> findByLicense_Tool_ToolId(Long toolId);

    List<LicenseAccount> findByStatusAndLicense_Tool_ToolId(LicenseAccount.Status status, Long licenseToolToolId);

    int countByLicense_Tool_ToolIdAndUsedFalse(Long toolId);

    @EntityGraph(attributePaths = {"license", "license.tool", "order"})
    @Query("""
        SELECT la FROM LicenseAccount la
        LEFT JOIN la.order o
        LEFT JOIN o.account a
        LEFT JOIN la.license l
        LEFT JOIN l.tool t
        WHERE a.accountId = :accountId
          AND (:status IS NULL OR la.status = :status)
          AND (:toolId IS NULL OR t.toolId = :toolId)
          AND (:loginMethod IS NULL OR t.loginMethod = :loginMethod)
          AND (:from IS NULL OR la.endDate >= :from)
          AND (:to IS NULL OR la.endDate <= :to)
          AND (
              :q IS NULL OR :q = '' OR
              LOWER(COALESCE(t.toolName, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
              LOWER(COALESCE(l.name, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
              LOWER(COALESCE(la.username, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
              LOWER(COALESCE(la.token, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
              LOWER(CONCAT(o.orderId, '')) LIKE LOWER(CONCAT('%', :q, '%'))
          )
    """)
    Page<LicenseAccount> findMyLicenseAccounts(
            @Param("accountId") Long accountId,
            @Param("q") String q,
            @Param("status") LicenseAccount.Status status,
            @Param("toolId") Long toolId,
            @Param("loginMethod") Tool.LoginMethod loginMethod,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"license", "license.tool", "license.tool.files", "order", "order.transaction"})
    Optional<LicenseAccount> findByLicenseAccountIdAndOrder_Account_AccountId(Long licenseAccountId, Long accountId);

    @EntityGraph(attributePaths = {"license", "license.tool", "license.tool.files", "order", "order.transaction"})
    Optional<LicenseAccount> findByOrder_OrderIdAndOrder_Account_AccountId(Long orderId, Long accountId);

    @EntityGraph(attributePaths = {"license", "license.tool", "order"})
    List<LicenseAccount> findByOrder_OrderId(Long orderId);
}
