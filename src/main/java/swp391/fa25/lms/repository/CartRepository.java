package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Cart;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByAccount(Account account);

    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items WHERE c.account = :account")
    Optional<Cart> findByAccountWithItems(@Param("account") Account account);
}
