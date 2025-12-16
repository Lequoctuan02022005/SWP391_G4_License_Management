package swp391.fa25.lms.controller.tool;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import swp391.fa25.lms.model.ToolReport;
import swp391.fa25.lms.service.ToolReportService;

import java.security.Principal;

@Controller
@RequestMapping("/tools/reports")
public class ToolReportController {

    @Autowired
    private ToolReportService reportService;


    // USER
    // Form gửi report
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/tool/{toolId}")
    public String showReportForm(@PathVariable Long toolId, Model model) {
        model.addAttribute("toolId", toolId);
        model.addAttribute("reasons", ToolReport.Reason.values());
        return "report/report-create";
    }

    // Submit report
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/tool/{toolId}")
    public String submitReport(@PathVariable Long toolId,
                               @RequestParam ToolReport.Reason reason,
                               @RequestParam(required = false) String description,
                               Principal principal) {

        reportService.createReport(toolId, reason, description, principal.getName());
        return "redirect:/tools/reports/my";
    }

    // User xem report của mình
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/my")
    public String myReports(Model model, Principal principal) {
        model.addAttribute("reports",
                reportService.getReportsByUser(principal.getName()));
        return "report/report-list";
    }

    // User đồng ý
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/{id}/agree")
    public String agree(@PathVariable Long id, Principal principal) {
        reportService.userAgree(id, principal.getName());
        return "redirect:/tools/reports/" + id;
    }

    // User không đồng ý
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/{id}/disagree")
    public String disagree(@PathVariable Long id, Principal principal) {
        reportService.userDisagree(id, principal.getName());
        return "redirect:/tools/reports/" + id;
    }


    // MANAGER / MODERATOR
    // Danh sách report (có filter status)
    @PreAuthorize("hasAnyRole('MANAGER','MODERATOR')")
    @GetMapping("/mod")
    public String modReports(
            @RequestParam(required = false) ToolReport.Status status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ToolReport> reports = reportService.getAllReports().isEmpty()
                ? Page.empty()
                : null; // placeholder (xem ghi chú bên dưới)

        // ⚠️ Ghi chú: nếu bạn muốn dùng paging + filter
        // thì gọi trực tiếp repo.filter(...) ở controller
        // hoặc bổ sung method paging trong service

        model.addAttribute("reports", reports);
        model.addAttribute("status", status);
        return "moderator/tool-report-list";
    }

    // Approve report
    @PreAuthorize("hasAnyRole('MANAGER','MODERATOR')")
    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id) {
        reportService.approveReport(id);
        return "redirect:/tools/reports/mod";
    }

    // Reject report
    @PreAuthorize("hasAnyRole('MANAGER','MODERATOR')")
    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id) {
        reportService.rejectReport(id);
        return "redirect:/tools/reports/mod";
    }

    // SELLER
    // Seller xem report liên quan đến tool của mình
    @PreAuthorize("hasRole('SELLER')")
    @GetMapping("/seller")
    public String sellerReports(Model model, Principal principal) {
        model.addAttribute("reports",
                reportService.getReportsForSeller(principal.getName()));
        return "seller/report-list";
    }

    // Seller xác nhận đang xử lý
    @PreAuthorize("hasRole('SELLER')")
    @PostMapping("/{id}/confirm")
    public String sellerConfirm(@PathVariable Long id, Principal principal) {
        reportService.sellerConfirmProcessing(id, principal.getName());
        return "redirect:/tools/reports/" + id;
    }

    // COMMON
    // Xem chi tiết report (dùng chung)
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public String reportDetail(@PathVariable Long id, Model model) {
        model.addAttribute("report", reportService.getReportDetail(id));
        return "report/report-detail";
    }
}
