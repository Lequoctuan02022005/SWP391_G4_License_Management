package swp391.fa25.lms.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.LicenseRenewLog;
import swp391.fa25.lms.repository.LicenseRenewLogRepository;

@Service
@RequiredArgsConstructor
public class LicenseRenewLogService {

    private final LicenseRenewLogRepository repo;

    public Page<LicenseRenewLog> findForCustomer(Long accountId, Pageable pageable) {
        return repo.findByCustomer(accountId, pageable);
    }

    public Page<LicenseRenewLog> findAll(Pageable pageable) {
        return repo.findAll(pageable);
    }
}