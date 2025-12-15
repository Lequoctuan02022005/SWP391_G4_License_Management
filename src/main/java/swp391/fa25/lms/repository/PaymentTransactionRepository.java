package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.PaymentTransaction;

import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    
    /**
     * Tìm transaction theo VNPay TxnRef
     */
    Optional<PaymentTransaction> findByVnpayTxnRef(String vnpayTxnRef);
}

