package swp391.fa25.lms.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.SellerSubscription;
import swp391.fa25.lms.repository.SellerSubscriptionRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SellerSubscriptionService {

    private final SellerSubscriptionRepository repo;

    public Page<SellerSubscription> findForSeller(Long sellerId, Pageable pageable) {
        return repo.findByAccountAccountId(sellerId, pageable);
    }
    // ====== FILTER (dùng cho renew-history) ======
    public Page<SellerSubscription> filter(
            String seller,
            Long packageId,
            String packageName,
            String status,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable
    ) {

        // xử lý seller
        String sellerFilter = null;
        if (seller != null && !seller.isBlank()) {
            sellerFilter = seller.trim();
        }
        String packageNameFilter = null;
        if (packageName != null && !packageName.isBlank()) {
            packageNameFilter = packageName.trim();
        }

        // xử lý status
        String statusFilter = null;
        if (status != null && !status.isBlank()) {
            statusFilter = status;
        }

        // xử lý fromDate
        LocalDateTime fromDateTime = null;
        if (fromDate != null) {
            fromDateTime = fromDate.atStartOfDay();
        }

        // xử lý toDate
        LocalDateTime toDateTime = null;
        if (toDate != null) {
            toDateTime = toDate.atTime(23, 59, 59);
        }

        return repo.filter(
                sellerFilter,
                packageId,
                packageNameFilter,
                statusFilter,
                fromDateTime,
                toDateTime,
                pageable
        );
    }

    // ====== ADMIN: xem tất cả (không filter) ======
    public Page<SellerSubscription> findAll(Pageable pageable) {
        return repo.findAll(pageable);
    }

    // ====== TÍNH TỔNG DOANH THU ======
    public Long sumRevenue(
            String seller,
            Long packageId,
            String status,
            LocalDate fromDate,
            LocalDate toDate
    ) {

        String sellerFilter = null;
        if (seller != null && !seller.isBlank()) {
            sellerFilter = seller.trim();
        }

        String statusFilter = null;
        if (status != null && !status.isBlank()) {
            statusFilter = status;
        }

        LocalDateTime fromDateTime = null;
        if (fromDate != null) {
            fromDateTime = fromDate.atStartOfDay();
        }

        LocalDateTime toDateTime = null;
        if (toDate != null) {
            toDateTime = toDate.atTime(23, 59, 59);
        }

        return repo.sumRevenue(
                sellerFilter,
                packageId,
                statusFilter,
                fromDateTime,
                toDateTime
        );
    }
}
