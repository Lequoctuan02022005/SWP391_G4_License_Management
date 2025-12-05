package swp391.fa25.lms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.ToolReport;

@Repository
public interface ToolReportRepository extends JpaRepository<ToolReport, Long> {

    @Query("SELECT r FROM ToolReport r " +
            "WHERE (:status IS NULL OR r.status = :status)")
    Page<ToolReport> filter(
            @Param("status") ToolReport.Status status,
            Pageable pageable
    );
}
