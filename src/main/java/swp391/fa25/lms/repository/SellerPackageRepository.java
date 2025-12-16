package swp391.fa25.lms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.SellerPackage;

import java.util.List;

@Repository
public interface SellerPackageRepository extends JpaRepository<SellerPackage, Integer> {
    List<SellerPackage> findByStatus(SellerPackage.Status status);

    @Query("""
        SELECT p FROM SellerPackage p
        WHERE (:name IS NULL OR LOWER(p.packageName) LIKE LOWER(CONCAT('%', :name, '%')))
          AND (:minPrice IS NULL OR p.price >= :minPrice)
          AND (:maxPrice IS NULL OR p.price <= :maxPrice)
          AND (:minDuration IS NULL OR p.durationInMonths >= :minDuration)
          AND (:maxDuration IS NULL OR p.durationInMonths <= :maxDuration)
          AND (:status IS NULL OR p.status = :status)
    """)
    Page<SellerPackage> filter(
            @Param("name") String name,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            @Param("minDuration") Integer minDuration,
            @Param("maxDuration") Integer maxDuration,
            @Param("status") SellerPackage.Status status,
            Pageable pageable
    );
}