package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.CustomerOrder;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {
    List<CustomerOrder> findByTransaction_TransactionId(Long transactionId);
    
    List<CustomerOrder> findByAccount_AccountId(Long accountId);
}

