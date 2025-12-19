package swp391.fa25.lms.controller.tool;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.Pattern;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.service.ToolFlowService;
import swp391.fa25.lms.service.ToolService;

import java.util.List;

@Controller
@RequestMapping("/tools/token")
@Validated
public class TokenController {

    private final ToolFlowService toolFlowService;
    private final ToolService toolService;

    public TokenController(ToolFlowService toolFlowService, ToolService toolService) {
        this.toolFlowService = toolFlowService;
        this.toolService = toolService;
    }

    // ================== ADD NEW TOOL (TOKEN) ==================

    @PreAuthorize("hasRole('SELLER')")
    @GetMapping("/manage")
    public String showTokenManage(HttpSession session, RedirectAttributes ra, Model model) {

        Account seller = (Account) session.getAttribute("loggedInAccount");
        if (seller == null) {
            ra.addFlashAttribute("error", "Please login first.");
            return "redirect:/login";
        }

        ToolFlowService.ToolSessionData pending =
                (ToolFlowService.ToolSessionData) session.getAttribute("pendingTool");

        if (pending == null) {
            ra.addFlashAttribute("error", "Session expired. Please create tool again.");
            return "redirect:/tools/seller/add";
        }

        // IMPORTANT: add mới => isEdit = false (để render CREATE form)
        model.addAttribute("isEdit", false);

        // tool-manage.html đang dùng ${tool...} => phải truyền Tool object
        model.addAttribute("tool", pending.getTool());
        model.addAttribute("tokens", pending.getTokens());

        return "tool/token-manage";
    }

    @PreAuthorize("hasRole('SELLER')")
    @PostMapping("/manage/submit")
    public String finalizeNewToolTokens(
            @RequestParam("tokens")
            List<@Pattern(regexp = "^[0-9]{6}$",
                    message = "Each token must contain exactly 6 digits") String> tokens,
            HttpSession session,
            RedirectAttributes ra
    ) {

        Account seller = (Account) session.getAttribute("loggedInAccount");
        if (seller == null) {
            ra.addFlashAttribute("error", "Please login first.");
            return "redirect:/login";
        }

        try {
            ToolFlowService.ToolSessionData pending =
                    (ToolFlowService.ToolSessionData) session.getAttribute("pendingTool");

            if (pending == null) {
                ra.addFlashAttribute("error", "Session expired. Please create the tool again.");
                return "redirect:/tools/seller/add";
            }

            toolFlowService.finalizeTokenTool(tokens, session);

            ra.addFlashAttribute("success", "✅ Tool created successfully!");
            return "redirect:/toollist";

        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/tools/token/manage";
        }
    }

    @PreAuthorize("hasRole('SELLER')")
    @PostMapping("/manage/back")
    public String cancelNewToolCreation(HttpSession session, RedirectAttributes ra) {

        Account seller = (Account) session.getAttribute("loggedInAccount");
        if (seller == null) {
            ra.addFlashAttribute("error", "Please login first.");
            return "redirect:/login";
        }

        // IMPORTANT: back add mới thì phải clear pendingTool
        toolFlowService.cancelToolCreation(session);

        ra.addFlashAttribute("info", "Tool creation canceled. Returning to add form.");
        return "redirect:/tools/seller/add";
    }

    // ================== EDIT TOOL TOKENS ==================

    @PreAuthorize("hasRole('SELLER')")
    @GetMapping("/edit")
    public String showEditTokenManage(HttpSession session, Model model, RedirectAttributes ra) {

        Account seller = (Account) session.getAttribute("loggedInAccount");
        if (seller == null) {
            ra.addFlashAttribute("error", "Please login first.");
            return "redirect:/login";
        }

        ToolFlowService.ToolSessionData pending =
                (ToolFlowService.ToolSessionData) session.getAttribute("pendingEditTool");

        if (pending == null) {
            ra.addFlashAttribute("error", "No edit session.");
            return "redirect:/toollist";
        }

        model.addAttribute("isEdit", true);
        model.addAttribute("tool", pending.getTool());
        model.addAttribute("tokens", pending.getTokens());

        return "tool/token-manage";
    }

    @PreAuthorize("hasRole('SELLER')")
    @PostMapping("/edit/finalize")
    public String finalizeEditTokens(
            @RequestParam("tokens") List<String> tokens,
            HttpSession session,
            RedirectAttributes ra
    ) {

        Account seller = (Account) session.getAttribute("loggedInAccount");
        if (seller == null) {
            ra.addFlashAttribute("error", "Please login first.");
            return "redirect:/login";
        }

        try {
            toolFlowService.finalizeEditTokenTool(tokens, session);
            ra.addFlashAttribute("success", "✅ Tokens and quantity updated successfully!");
            return "redirect:/toollist";

        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/tools/token/edit";
        }
    }

    @PreAuthorize("hasRole('SELLER')")
    @PostMapping("/edit/back")
    public String handleBackFromEdit(HttpSession session, RedirectAttributes ra) {

        Account seller = (Account) session.getAttribute("loggedInAccount");
        if (seller == null) {
            ra.addFlashAttribute("error", "Please login first.");
            return "redirect:/login";
        }

        toolFlowService.cancelToolCreation(session);
        ra.addFlashAttribute("info", "Token edit canceled. Returning to tools.");
        return "redirect:/toollist";
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public String handleTokenValidationError(
            jakarta.validation.ConstraintViolationException ex,
            RedirectAttributes ra,
            jakarta.servlet.http.HttpServletRequest request
    ) {

        String message = ex.getConstraintViolations().stream()
                .map(v -> v.getMessage())
                .findFirst()
                .orElse("Invalid token format");

        ra.addFlashAttribute("error", message);

        if (request.getRequestURI().contains("/edit")) {
            return "redirect:/tools/token/edit";
        }
        return "redirect:/tools/token/manage";
    }
}
