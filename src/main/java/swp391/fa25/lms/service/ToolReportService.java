package swp391.fa25.lms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.model.ToolReport;
import swp391.fa25.lms.repository.AccountRepository;
import swp391.fa25.lms.repository.ToolReportRepository;
import swp391.fa25.lms.repository.ToolRepository;

import java.util.List;

@Service
public class ToolReportService {

    @Autowired
    private ToolReportRepository reportRepo;

    @Autowired
    private ToolRepository toolRepo;

    @Autowired
    private AccountRepository accountRepo;


    //User
    // User gửi report
    public void createReport(Long toolId,
                             ToolReport.Reason reason,
                             String description,
                             String reporterEmail) {

        Account reporter = accountRepo.findByEmail(reporterEmail)
                .orElseThrow(() -> new RuntimeException("Reporter not found"));

        Tool tool = toolRepo.findById(toolId)
                .orElseThrow(() -> new RuntimeException("Tool not found"));

        ToolReport report = new ToolReport(reporter, tool, reason, description);
        reportRepo.save(report);
    }

    // User xem danh sách report của mình
    public List<ToolReport> getReportsByUser(String email) {
        return reportRepo.findByReporter_Email(email);
    }

    // User đồng ý kết quả
    public void userAgree(Long reportId, String email) {
        ToolReport report = getReportDetail(reportId);

        if (!report.getReporter().getEmail().equals(email)) {
            throw new RuntimeException("No permission");
        }

        report.setStatus(ToolReport.Status.AGREED);
        reportRepo.save(report);
    }

    // User không đồng ý kết quả
    public void userDisagree(Long reportId, String email) {
        ToolReport report = getReportDetail(reportId);

        if (!report.getReporter().getEmail().equals(email)) {
            throw new RuntimeException("No permission");
        }

        report.setStatus(ToolReport.Status.DISAGREED);
        reportRepo.save(report);
    }

    //Manager/Mod
    // Manager xem toàn bộ report
    public List<ToolReport> getAllReports() {
        return reportRepo.findAll();
    }

    // Manager duyệt report
    public void approveReport(Long reportId) {
        ToolReport report = getReportDetail(reportId);
        report.setStatus(ToolReport.Status.APPROVED);
        reportRepo.save(report);
    }

    // Manager từ chối report
    public void rejectReport(Long reportId) {
        ToolReport report = getReportDetail(reportId);
        report.setStatus(ToolReport.Status.REJECTED);
        reportRepo.save(report);
    }


    //Seller
    // Seller xem report liên quan đến tool của mình
    public List<ToolReport> getReportsForSeller(String email) {
        return reportRepo.findByTool_Seller_Email(email);
    }

    // Seller xác nhận đang xử lý
    public void sellerConfirmProcessing(Long reportId, String email) {
        ToolReport report = getReportDetail(reportId);

        if (!report.getTool().getSeller().getEmail().equals(email)) {
            throw new RuntimeException("No permission");
        }

        report.setStatus(ToolReport.Status.PROCESSING);
        reportRepo.save(report);
    }

    //Common
    // Xem chi tiết report
    public ToolReport getReportDetail(Long reportId) {
        return reportRepo.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));
    }
}
