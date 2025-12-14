package swp391.fa25.lms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.SellerSubscription;

import java.util.List;

@Repository
public interface SellerSubscriptionRepository extends JpaRepository<SellerSubscription, Long> {

    // Lấy danh sách subscription của seller theo thứ tự mới → cũ
    List<SellerSubscription> findByAccountOrderByStartDateDesc(Account account);

    Page<SellerSubscription> findByAccountAccountId(Long accountId, Pageable pageable);
}
