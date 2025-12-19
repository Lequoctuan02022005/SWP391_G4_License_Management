package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.OrderLicense;

import java.util.List;

@Repository
public interface OrderLicenseRepository extends JpaRepository<OrderLicense, Long> {
    List<OrderLicense> findByOrder_OrderId(Long orderId);
}
