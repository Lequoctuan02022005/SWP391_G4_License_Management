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

    // ========== LIST + FILTER + PAGINATION ==========
    public Page<SellerPackage> filterPackages(
            String packageName,
            Double minPrice,
            Double maxPrice,
            Integer minDuration,
            Integer maxDuration,
            SellerPackage.Status status,
            Pageable pageable
    ) {
        List<SellerPackage> filtered = packageRepo.findAll().stream()
                .filter(p -> packageName == null || p.getPackageName()
                        .toLowerCase().contains(packageName.toLowerCase()))
                .filter(p -> minPrice == null || p.getPrice() >= minPrice)
                .filter(p -> maxPrice == null || p.getPrice() <= maxPrice)
                .filter(p -> minDuration == null || p.getDurationInMonths() >= minDuration)
                .filter(p -> maxDuration == null || p.getDurationInMonths() <= maxDuration)
                .filter(p -> status == null || p.getStatus() == status)
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());

        List<SellerPackage> pageContent =
                start > filtered.size()
                        ? List.of()
                        : filtered.subList(start, end);

        return new PageImpl<>(pageContent, pageable, filtered.size());
    }

    // ========== FIND BY ID ==========
    public SellerPackage getById(int id) {
        return packageRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Seller package not found"));
    }

    // ========== CREATE / UPDATE ==========
    public SellerPackage save(SellerPackage pkg) {
        return packageRepo.save(pkg);
    }
}
