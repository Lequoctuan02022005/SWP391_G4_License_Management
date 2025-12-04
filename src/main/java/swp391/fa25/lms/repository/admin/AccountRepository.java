package swp391.fa25.lms.repository.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.Account;

import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    boolean existsByEmail(String email);

    Account findByEmail(String email);

    @Query("SELECT a FROM Account a " +
            "WHERE a.email LIKE %:keyword% " +
            "OR a.fullName LIKE %:keyword% " +
            "OR a.phone LIKE %:keyword% " +
            "OR a.address LIKE %:keyword%")
    Page<Account> searchPage(@Param("keyword") String keyword, Pageable pageable);
}
