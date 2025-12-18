package swp391.fa25.lms.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.SellerSubscription;
import swp391.fa25.lms.repository.SellerSubscriptionRepository;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class SellerSubscriptionService {

    private final SellerSubscriptionRepository repo;

    public Page<SellerSubscription> findForSeller(Long sellerId, Pageable pageable) {
        return repo.findByAccountAccountId(sellerId, pageable);
    }

    public Page<SellerSubscription> filter(
            String seller,
            Long packageId,
            String status,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable
    ) {

        return repo.filter(
                seller == null || seller.isBlank() ? null : seller.trim(),
                packageId,
                status == null || status.isBlank() ? null : status,
                fromDate != null ? fromDate.atStartOfDay() : null,
                toDate != null ? toDate.atTime(23, 59, 59) : null,
                pageable
        );
    }
    public Page<SellerSubscription> findAll(Pageable pageable) {
        return repo.findAll(pageable);
    }
    public Long sumRevenue(
            String seller,
            Long packageId,
            String status,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        return repo.sumRevenue(
                seller == null || seller.isBlank() ? null : seller,
                packageId,
                status == null || status.isBlank() ? null : status,
                fromDate != null ? fromDate.atStartOfDay() : null,
                toDate != null ? toDate.atTime(23, 59, 59) : null
        );
    }
}
