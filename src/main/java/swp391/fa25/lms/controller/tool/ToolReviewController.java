package swp391.fa25.lms.controller.tool;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.model.ToolFile;
import swp391.fa25.lms.service.ToolReviewService;
import swp391.fa25.lms.service.CategoryService;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
@RequestMapping
public class ToolReviewController {

    private final ToolReviewService toolReviewService;
    private final CategoryService categoryService;

    // ========================= MODERATOR =========================

    @GetMapping("/moderator/upload-requests")
    public String moderatorPendingList(
            @RequestParam(required = false) Long sellerId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime uploadFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime uploadTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model
    ) {

        var toolPage = toolReviewService.getModeratorPendingTools(
                sellerId, categoryId, keyword, uploadFrom, uploadTo, page, size
        );

        model.addAttribute("toolPage", toolPage);
        model.addAttribute("categories", categoryService.getAllCategories());

        // giữ filter
        model.addAttribute("sellerId", sellerId);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("keyword", keyword);
        model.addAttribute("uploadFrom", uploadFrom);
        model.addAttribute("uploadTo", uploadTo);

        model.addAttribute("activePage", "uploadRequest");
        return "tool/tool-upload-list";
    }

    @GetMapping("/moderator/tool/{id}")
    public String moderatorToolDetail(@PathVariable Long id,
                                      HttpServletRequest request,
                                      Model model) {
        Tool tool = toolReviewService.getToolOrThrow(id);

        boolean hasWrappedFile = tool.getFiles() != null
                && tool.getFiles().stream()
                .anyMatch(f -> f.getFileType() == ToolFile.FileType.WRAPPED);

        model.addAttribute("tool", tool);
        model.addAttribute("hasWrappedFile", hasWrappedFile);
        model.addAttribute("account", getSessionAccount(request));
        return "tool/tool-upload-detail";
    }

    @PostMapping("/moderator/tool/{id}/approve")
    public String moderatorApprove(
            @PathVariable Long id,
            HttpServletRequest request,
            RedirectAttributes redirect
    ) {
        Account acc = getSessionAccount(request);
        toolReviewService.moderatorApprove(id, acc);
        redirect.addFlashAttribute("message", "Approved tool successfully!");
        return "redirect:/moderator/upload-requests";
    }

    @PostMapping("/moderator/tool/{id}/reject")
    public String moderatorReject(
            @PathVariable Long id,
            @RequestParam("note") String reason,
            HttpServletRequest request,
            RedirectAttributes redirect
    ) {
        Account acc = getSessionAccount(request);
        toolReviewService.moderatorReject(id, reason, acc);
        redirect.addFlashAttribute("message", "Rejected tool successfully!");
        return "redirect:/moderator/upload-requests";
    }

    // ========================= MANAGER =========================

    @GetMapping("/manager/upload-tools")
    public String managerApprovedList(
            @RequestParam(required = false) Long sellerId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime uploadFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime uploadTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model
    ) {

        var toolPage = toolReviewService.getManagerApprovedTools(
                sellerId, categoryId, keyword, uploadFrom, uploadTo, page, size
        );

        model.addAttribute("toolPage", toolPage);
        model.addAttribute("categories", categoryService.getAllCategories());

        model.addAttribute("sellerId", sellerId);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("keyword", keyword);
        model.addAttribute("uploadFrom", uploadFrom);
        model.addAttribute("uploadTo", uploadTo);

        model.addAttribute("activePage", "uploadTool");
        return "tool/tool-upload-list";
    }

    @GetMapping("/manager/tool/{id}")
    public String managerToolDetail(@PathVariable Long id,
                                    HttpServletRequest request,
                                    Model model) {
        Tool tool = toolReviewService.getToolOrThrow(id);
        model.addAttribute("tool", tool);
        model.addAttribute("account", getSessionAccount(request));
        return "tool/tool-upload-detail";
    }

    @PostMapping("/manager/tool/{id}/publish")
    public String managerPublish(
            @PathVariable Long id,
            HttpServletRequest request,
            RedirectAttributes redirect
    ) {
        Account acc = getSessionAccount(request);
        toolReviewService.managerPublish(id, acc);
        redirect.addFlashAttribute("message", "Tool published!");
        return "redirect:/manager/upload-tools";
    }

    @PostMapping("/manager/tool/{id}/pending")
    public String managerSetPending(
            @PathVariable Long id,
            @RequestParam("note") String note,
            HttpServletRequest request,
            RedirectAttributes redirect
    ) {
        Account acc = getSessionAccount(request);
        toolReviewService.managerSetPending(id, note, acc);
        redirect.addFlashAttribute("message", "Set tool to pending.");
        return "redirect:/manager/tool/" + id;
    }

    @PostMapping("/manager/tool/{id}/reject")
    public String managerReject(
            @PathVariable Long id,
            @RequestParam("note") String note,
            HttpServletRequest request,
            RedirectAttributes redirect
    ) {
        Account acc = getSessionAccount(request);
        toolReviewService.managerReject(id, note, acc);
        redirect.addFlashAttribute("message", "Rejected tool.");
        return "redirect:/manager/upload-tools";
    }

    // ========================= COMMON =========================
    private Account getSessionAccount(HttpServletRequest request) {
        Account acc = (Account) request.getSession().getAttribute("loggedInAccount");
        if (acc == null) {
            throw new RuntimeException("Session expired or not logged in");
        }
        return acc;
    }
    @PostMapping("/moderator/tool/{id}/upload-file")
    public String uploadWrappedFile(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request,
            RedirectAttributes ra
    ) {

        Account mod = getSessionAccount(request);

        toolReviewService.modUploadWrappedFile(id, file, mod);

        ra.addFlashAttribute("message", "Wrapped file uploaded successfully.");
        return "redirect:/moderator/tool/" + id;
    }
}
