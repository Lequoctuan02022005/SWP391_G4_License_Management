package swp391.fa25.lms.controller.tool;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.model.ToolReport;
import swp391.fa25.lms.repository.mod.ToolReportRepository;
import swp391.fa25.lms.repository.mod.ToolRepository;

import java.security.Principal;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/tools")
public class ToolController {

    @Autowired
    private ToolRepository toolRepo;

    @Autowired
    private ToolReportRepository reportRepo;



    // 1. LIST PENDING TOOL UPLOADS
    @GetMapping("/mod/uploads")
    public String listPendingUploads(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by("createdAt").descending());

        Page<Tool> tools = toolRepo.findPendingUploads(keyword, pageable);

        model.addAttribute("tools", tools);
        model.addAttribute("keyword", keyword);

        return "mod/tool-upload-list";
    }


    // 2. VIEW PENDING TOOL DETAIL
    @GetMapping("/mod/uploads/{id}")
    public String viewUploadDetail(@PathVariable Long id, Model model) {

        Tool tool = toolRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Tool not found"));

        model.addAttribute("tool", tool);
        return "mod/tool-upload-detail";
    }


    // 3. APPROVE TOOL UPLOAD
    @PostMapping("/mod/uploads/{id}/approve")
    public String approveUpload(
            @PathVariable Long id,
            Principal principal,
            RedirectAttributes ra) {

        Tool tool = toolRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Tool not found"));

        String reviewer = (principal != null) ? principal.getName() : "SYSTEM";

        tool.setStatus(Tool.Status.APPROVED);
        tool.setReviewedBy(reviewer);
        tool.setUpdatedAt(LocalDateTime.now());

        toolRepo.save(tool);

        ra.addFlashAttribute("success", "Tool approved successfully.");
        return "redirect:/tools/mod/uploads";
    }


    // 4. REJECT TOOL UPLOAD
    @PostMapping("/mod/uploads/{id}/reject")
    public String rejectUpload(
            @PathVariable Long id,
            @RequestParam String note,
            Principal principal,
            RedirectAttributes ra) {

        Tool tool = toolRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Tool not found"));

        String reviewer = (principal != null) ? principal.getName() : "SYSTEM";

        tool.setStatus(Tool.Status.REJECTED);
        tool.setNote(note);
        tool.setReviewedBy(reviewer);
        tool.setUpdatedAt(LocalDateTime.now());

        toolRepo.save(tool);

        ra.addFlashAttribute("success", "Tool rejected.");
        return "redirect:/tools/mod/uploads";
    }


    // 5. LIST REPORTS
    @GetMapping("/mod/reports")
    public String listReports(
            @RequestParam(required = false) ToolReport.Status status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by("reportedAt").descending());

        Page<ToolReport> reports = reportRepo.filter(status, pageable);

        model.addAttribute("reports", reports);
        model.addAttribute("status", status);

        return "mod/tool-report-list";
    }


    // 6. VIEW REPORT DETAIL
    @GetMapping("/mod/reports/{id}")
    public String reportDetail(@PathVariable Long id, Model model) {

        ToolReport report = reportRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        model.addAttribute("report", report);
        return "mod/tool-report-detail";
    }


    // 7. APPROVE REPORT
    @PostMapping("/mod/reports/{id}/approve")
    public String approveReport(@PathVariable Long id,
                                RedirectAttributes ra) {

        ToolReport report = reportRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        report.setStatus(ToolReport.Status.APPROVED);
        reportRepo.save(report);

        ra.addFlashAttribute("success", "Report approved.");
        return "redirect:/tools/mod/reports";
    }


    // 8. REJECT REPORT
    @PostMapping("/mod/reports/{id}/reject")
    public String rejectReport(@PathVariable Long id,
                               RedirectAttributes ra) {

        ToolReport report = reportRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        report.setStatus(ToolReport.Status.REJECTED);
        reportRepo.save(report);

        ra.addFlashAttribute("success", "Report rejected.");
        return "redirect:/tools/mod/reports";
    }
}
