package swp391.fa25.lms.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.LicenseRenewLog;
import swp391.fa25.lms.repository.LicenseRenewLogRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LicenseRenewLogService {

    private final LicenseRenewLogRepository repo;
    // ===== CUSTOMER =====
    public Page<LicenseRenewLog> filterForCustomer(
            Long customerId,
            LocalDate fromDate,
            LocalDate toDate,
            Long minAmount,
            Long maxAmount,
            Pageable pageable
    ) {
        return repo.filterForCustomer(
                customerId,
                toStart(fromDate),
                toEnd(toDate),
                minAmount,
                maxAmount,
                pageable
        );
    }
    // ===== SELLER =====
    public Page<LicenseRenewLog> filterForSeller(
            Long sellerId,
            LocalDate fromDate,
            LocalDate toDate,
            Long minAmount,
            Long maxAmount,
            Pageable pageable
    ) {
        return repo.filterForSeller(
                sellerId,
                toStart(fromDate),
                toEnd(toDate),
                minAmount,
                maxAmount,
                pageable
        );
    }

    private LocalDateTime toStart(LocalDate date) {
        if (date == null) return null;
        return date.atStartOfDay();
    }

    private LocalDateTime toEnd(LocalDate date) {
        if (date == null) return null;
        return date.atTime(23, 59, 59);
    }
    public Page<LicenseRenewLog> findAll(Pageable pageable) {
        return repo.findAll(pageable);
    }
}