package swp391.fa25.lms.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.SellerPackage;
import swp391.fa25.lms.repository.SellerPackageRepository;

import java.util.List;
@Service
@RequiredArgsConstructor
public class SellerPackageService {

    private final SellerPackageRepository packageRepo;

    public Page<SellerPackage> filter(
            String name,
            Double minPrice,
            Double maxPrice,
            Integer minDuration,
            Integer maxDuration,
            SellerPackage.Status status,
            Pageable pageable
    ) {
        return packageRepo.filter(name, minPrice, maxPrice,
                minDuration, maxDuration, status, pageable);
    }

    // FIND
    public SellerPackage getById(int id) {
        return packageRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Seller package not found"));
    }

    // SAVE (ADD + EDIT)
    public void save(SellerPackage pkg) {
        if (pkg.getId() == 0) {
            // ADD
            pkg.setStatus(SellerPackage.Status.ACTIVE);
        } else {
            // EDIT → giữ status cũ nếu null
            SellerPackage old = getById(pkg.getId());
            if (pkg.getStatus() == null) {
                pkg.setStatus(old.getStatus());
            }
        }
        packageRepo.save(pkg);
    }
}
