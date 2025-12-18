package swp391.fa25.lms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Account.AccountStatus;
import swp391.fa25.lms.model.Role.RoleName;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByEmail(String email);

    boolean existsByEmail(String email);

    Page<Account> findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(
            String email, String fullName, Pageable pageable);

    boolean existsByEmailAndVerifiedTrue(String email);

    Optional<Account> findByVerificationCode(String code);

    long countByRole_RoleName(RoleName roleName);

    Page<Account> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Account> findByStatusOrderByUpdatedAtDesc(AccountStatus status, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Account a SET a.status = :status, a.updatedAt = CURRENT_TIMESTAMP WHERE a.accountId = :id")
    int updateStatus(@Param("id") long id, @Param("status") AccountStatus status);

    @Query("""
           SELECT a FROM Account a
           WHERE (:q IS NULL OR :q = '' 
                 OR LOWER(a.email) LIKE LOWER(CONCAT('%', :q, '%'))
                 OR LOWER(a.fullName) LIKE LOWER(CONCAT('%', :q, '%')))
             AND (:status IS NULL OR a.status = :status)
           """)
    Page<Account> search(@Param("q") String q,
                         @Param("status") AccountStatus status,
                         Pageable pageable);

    List<Account> findTop8ByStatusOrderByUpdatedAtDesc(AccountStatus status);

    @Query("""
           select a.accountId
           from Account a
           where lower(a.email) = lower(:email)
           """)
    Optional<Long> findIdByEmail(@Param("email") String email);

    Optional<Account> findByEmailIgnoreCase(String email);

    @Query("SELECT a FROM Account a JOIN a.role r WHERE r.roleId = 2 or r.roleId = 1")
    List<Account> findAllSellers();

    // Search with multiple filters (keyword, role, status)
    @Query("""
           SELECT a FROM Account a
           WHERE (:keyword IS NULL OR :keyword = '' 
                 OR LOWER(a.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(a.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')))
             AND (:roleId IS NULL OR a.role.roleId = :roleId)
             AND (:status IS NULL OR a.status = :status)
           ORDER BY a.createdAt DESC
           """)
    Page<Account> searchWithFilters(@Param("keyword") String keyword,
                                     @Param("roleId") Integer roleId,
                                     @Param("status") AccountStatus status,
                                     Pageable pageable);

}
