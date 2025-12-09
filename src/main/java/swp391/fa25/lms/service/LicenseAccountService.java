package swp391.fa25.lms.service;

import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.LicenseAccount;
import swp391.fa25.lms.repository.LicenseAccountRepository;
import swp391.fa25.lms.repository.LicenseToolRepository;

import java.util.List;

/**
 * Xử lý: tạo license account sau thanh toán, xem/điều chỉnh password/token,
 * mark activated/revoke, và build view model cho trang my-license.
 */
@Service
public class LicenseAccountService {

    private final LicenseAccountRepository accRepo;
    private final LicenseToolRepository licenseRepo;

    public LicenseAccountService(LicenseAccountRepository accRepo,
                                 LicenseToolRepository licenseRepo) {
        this.accRepo = accRepo;
        this.licenseRepo = licenseRepo;
    }


    public List<LicenseAccount> getActiveLicenses(long toolId) {
        return accRepo.findByStatusAndLicense_Tool_ToolId(LicenseAccount.Status.ACTIVE, toolId);
    }
}