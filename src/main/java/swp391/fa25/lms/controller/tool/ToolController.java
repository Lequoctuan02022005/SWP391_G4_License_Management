package swp391.fa25.lms.controller.tool;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Role;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.service.FileStorageService;
import swp391.fa25.lms.service.ToolFlowService;
import swp391.fa25.lms.service.ToolService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.*;
import swp391.fa25.lms.model.ToolReport;
import swp391.fa25.lms.repository.ToolReportRepository;
import swp391.fa25.lms.repository.ToolRepository;

import java.security.Principal;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/tools")
public class ToolController {

    // ========== SELLER DEPENDENCIES ==========
    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private ToolService toolService;

    @Autowired
    private ToolFlowService toolFlowService;

    private Account requireActiveSeller(HttpSession session, RedirectAttributes redirectAttrs) {
        Account seller = (Account) session.getAttribute("loggedInAccount");
        if (seller == null) {
            redirectAttrs.addFlashAttribute("error", "Please login again.");
            return null;
        }
//        if (!accountService.isSellerActive(seller)) {
//            redirectAttrs.addFlashAttribute("error", "Your seller package has expired. Please renew before continuing.");
//            return null;
//        }
        return seller;
    }
    /**
     *  Trang danh sách Tool của seller
     * GET /tools/seller
     */
    @PreAuthorize("hasRole('SELLER')")
    @GetMapping("/seller/add")
    public String showAddToolForm(
            @RequestParam(value = "cancel", required = false) Boolean cancel,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttrs
    ) {
        if (cancel != null && cancel) {
            toolFlowService.cancelToolCreation(session);
            redirectAttrs.addFlashAttribute("info", "Creation canceled.");
            return "redirect:/tools/seller/add";
        }

        ToolFlowService.ToolSessionData pending =
                (ToolFlowService.ToolSessionData) session.getAttribute("pendingTool");
        if (pending != null) {
            model.addAttribute("tool", pending.getTool());
            model.addAttribute("licenses", pending.getLicenses());
            model.addAttribute("categoryId", pending.getCategory().getCategoryId());
            model.addAttribute("restoreFromSession", true);
        } else {
            model.addAttribute("tool", new Tool());
        }

        model.addAttribute("categories", toolService.getAllCategories());
        return "tool/tool-add";
    }
    /**
     *  Xử lý submit Add Tool
     * POST /tools/seller/add
     */
    @PreAuthorize("hasRole('SELLER')")
    @PostMapping("/seller/add")
    public String addTool(
            @Valid @ModelAttribute("tool") Tool tool,
            BindingResult result,
            @RequestParam("imageFile") MultipartFile imageFile,
            @RequestParam("toolFile") MultipartFile toolFile,
            @RequestParam("licenseDays") List<Integer> licenseDays,
            @RequestParam("licensePrices") List<Double> licensePrices,
            HttpSession session,
            RedirectAttributes redirectAttrs,
            Model model
    ) {
        Account seller = requireActiveSeller(session, redirectAttrs);
        if (seller == null) {
            return "redirect:/login";
        }

        if (result.hasErrors()) {
            model.addAttribute("categories", toolService.getAllCategories());
            return "tool/tool-add";
        }

        try {
            toolFlowService.startCreateTool(
                    tool,
                    imageFile,
                    toolFile,
                    tool.getCategory().getCategoryId(),
                    licenseDays,
                    licensePrices,
                    session
            );

            if (tool.getLoginMethod() == Tool.LoginMethod.TOKEN) {
                redirectAttrs.addFlashAttribute("info", "Please add tokens to finalize your tool.");
                return "redirect:/tools/token/manage";
            }

            redirectAttrs.addFlashAttribute("success", "Tool created successfully!");
            return "redirect:/toollist";

        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tools/seller/add";
    }

    /**
     *  Form Edit Tool cho seller
     * GET /tools/seller/edit/{id}
     */
    @PreAuthorize("hasRole('SELLER')")
    @GetMapping("/seller/edit/{id}")
    public String showEditToolForm(
            @PathVariable Long id,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttrs
    ) {
        Account seller = requireActiveSeller(session, redirectAttrs);
        if (seller == null) {
            return "redirect:/login";
        }

        Tool tool = toolService.getToolByIdAndSeller(id, seller);
        if (tool == null) {
            redirectAttrs.addFlashAttribute("success", "Tool created successfully!");
            return "redirect:/toollist";
        }

        model.addAttribute("tool", tool);
        model.addAttribute("categories", toolService.getAllCategories());
        model.addAttribute("isEdit", true);
        model.addAttribute("isTokenLogin", tool.getLoginMethod() == Tool.LoginMethod.TOKEN);
        return "tool/tool-edit";
    }
    @PreAuthorize("hasRole('SELLER')")
    @PostMapping("/seller/edit/{id}")
    public String updateTool(
            @PathVariable Long id,
            @ModelAttribute("tool") Tool updatedTool,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "toolFile", required = false) MultipartFile toolFile,
            @RequestParam(value = "licenseDays", required = false) List<Integer> licenseDays,
            @RequestParam(value = "licensePrices", required = false) List<Double> licensePrices,
            @RequestParam(value = "action", required = false) String action,
            HttpSession session,
            RedirectAttributes redirectAttrs
    ) {

        Account seller = requireActiveSeller(session, redirectAttrs);
        if (seller == null) {
            return "redirect:/login";
        }

        try {
            Tool existing = toolService.getToolByIdAndSeller(id, seller);
            if (existing == null) {
                throw new IllegalArgumentException("Tool not found or unauthorized.");
            }

            String imagePath = null;
            String toolPath = null;

            if (imageFile != null && !imageFile.isEmpty()) {
                imagePath = fileStorageService.uploadImage(imageFile);
            }
            if (toolFile != null && !toolFile.isEmpty()) {
                toolPath = fileStorageService.uploadToolFile(toolFile);
            }

            // Nếu loginMethod = TOKEN và action=token => chuyển sang flow token-edit
            if (Objects.equals(action, "token") && existing.getLoginMethod() == Tool.LoginMethod.TOKEN) {
                List<Integer> days = (licenseDays != null) ? licenseDays : new ArrayList<>();
                List<Double> prices = (licensePrices != null) ? licensePrices : new ArrayList<>();

                toolFlowService.startEditToolSession(
                        existing,
                        updatedTool,
                        imageFile,
                        toolFile,
                        days,
                        prices,
                        session
                );
                redirectAttrs.addFlashAttribute("info", "Please review and update tokens for this tool.");
                return "redirect:/tools/token/edit";
            }

            // USER_PASSWORD → cập nhật trực tiếp
            toolService.updateTool(
                    id,
                    updatedTool,
                    imagePath,
                    toolPath,
                    licenseDays != null ? licenseDays : new ArrayList<>(),
                    licensePrices != null ? licensePrices : new ArrayList<>(),
                    seller
            );

            redirectAttrs.addFlashAttribute("success", "Tool updated successfully!");
            return "redirect:/tools/seller";

        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/tools/seller/edit/" + id;
        }
    }
    @GetMapping("/detail/{id}")
    public String viewToolDetail(
            @PathVariable Long id,
            Model model,
            HttpSession session
    ) {
        Tool tool = toolService.getToolById(id);
        if (tool == null) {
            return "redirect:/error";
        }

        Account user = (Account) session.getAttribute("loggedInAccount");

        boolean isCustomer = (user != null && user.getRole().getRoleName() == Role.RoleName.CUSTOMER);

        model.addAttribute("tool", tool);
        model.addAttribute("licenses", tool.getLicenses());
        model.addAttribute("isCustomer", isCustomer);

        return "tool/tool-detail";
    }
    @PostMapping("/seller/add/cancel")
    public String cancelAddTool(HttpSession session, RedirectAttributes ra) {
        toolFlowService.cancelToolCreation(session);
        ra.addFlashAttribute("info", "Tool creation canceled.");
        return "redirect:/toollist";
    }

    @Autowired
    private ToolRepository toolRepo;

    @Autowired
    private ToolReportRepository reportRepo;


    // 1. LIST ALL TOOL UPLOADS
    @PreAuthorize("hasRole('MOD')")
    @GetMapping("/mod/uploads")
    public String listUploads(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Tool.Status status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by("createdAt").descending());

        Page<Tool> tools = toolRepo.filterTools(keyword, status, pageable);

        model.addAttribute("tools", tools);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status != null ? status.name() : "");

        return "moderator/tool-upload-list";
    }



    // 2. VIEW TOOL DETAIL
    @PreAuthorize("hasRole('MOD')")
    @GetMapping("/mod/uploads/{id}")
    public String viewUploadDetail(@PathVariable Long id, Model model) {

        Tool tool = toolRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Tool not found"));

        model.addAttribute("tool", tool);
        return "moderator/tool-upload-detail";
    }


    // 3. APPROVE TOOL UPLOAD
    @PreAuthorize("hasRole('MOD')")
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
    @PreAuthorize("hasRole('MOD')")
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
    @PreAuthorize("hasRole('MOD')")
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

        return "moderator/tool-report-list";
    }


    // 6. VIEW REPORT DETAIL
    @PreAuthorize("hasRole('MOD')")
    @GetMapping("/mod/reports/{id}")
    public String reportDetail(@PathVariable Long id, Model model) {

        ToolReport report = reportRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        model.addAttribute("report", report);
        return "moderator/tool-report-detail";
    }


    // 7. APPROVE REPORT
    @PreAuthorize("hasRole('MOD')")
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
    @PreAuthorize("hasRole('MOD')")
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
