package swp391.fa25.lms.controller.tool;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
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
import swp391.fa25.lms.repository.FeedbackRepository;
import swp391.fa25.lms.model.Feedback;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/tools")
public class ToolController {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private ToolService toolService;

    @Autowired
    private ToolFlowService toolFlowService;

    @Autowired
    private FeedbackRepository feedbackRepo;

    /* ===========================================================
       SELLER CHECK
     =========================================================== */
    private boolean isExpiredSeller(Account acc) {
        return acc == null
                || !Boolean.TRUE.equals(acc.getSellerActive())
                || acc.getSellerExpiryDate() == null
                || acc.getSellerExpiryDate().isBefore(LocalDateTime.now());
    }

    private Account requireActiveSeller(HttpSession session, RedirectAttributes ra) {
        Account seller = (Account) session.getAttribute("loggedInAccount");
        if (seller == null || isExpiredSeller(seller)) {
            ra.addFlashAttribute("error", "Seller session invalid or expired.");
            return null;
        }
        return seller;
    }

    /* ===========================================================
       ADD TOOL
     =========================================================== */
    @PreAuthorize("hasRole('SELLER')")
    @GetMapping("/seller/add")
    public String showAddToolForm(
            @RequestParam(value = "cancel", required = false) Boolean cancel,
            Model model,
            HttpSession session,
            RedirectAttributes ra
    ) {

        if (cancel != null && cancel) {
            toolFlowService.cancelToolCreation(session);
            ra.addFlashAttribute("info", "Creation canceled.");
            return "redirect:/tools/seller/add";
        }

        ToolFlowService.ToolSessionData pending =
                (ToolFlowService.ToolSessionData) session.getAttribute("pendingTool");

        if (pending != null) {
            Tool tool = new Tool();
            tool.setToolName(pending.getTool().getToolName());
            tool.setDescription(pending.getTool().getDescription());
            tool.setNote(pending.getTool().getNote());
            tool.setCategory(pending.getCategory());

            model.addAttribute("tool", tool);
            model.addAttribute("licenses", pending.getLicenses());
            model.addAttribute("restoreFromSession", true);
        } else {
            model.addAttribute("tool", new Tool());
        }

        model.addAttribute("categories", toolService.getAllCategories());
        return "tool/tool-add";
    }

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
            RedirectAttributes ra,
            Model model
    ) {

        Account seller = requireActiveSeller(session, ra);
        if (seller == null) return "redirect:/seller/renew";

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
                ra.addFlashAttribute("info", "Please manage tokens to finalize tool.");
                return "redirect:/tools/token/manage";
            }

            ra.addFlashAttribute("success", "Tool created successfully.");
            return "redirect:/toollist";

        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("categories", toolService.getAllCategories());
            return "tool/tool-add";
        }
    }

    /* ===========================================================
       EDIT TOOL
     =========================================================== */
    @PreAuthorize("hasRole('SELLER')")
    @GetMapping("/seller/edit/{id}")
    public String showEditToolForm(
            @PathVariable Long id,
            Model model,
            HttpSession session,
            RedirectAttributes ra
    ) {

        Account seller = requireActiveSeller(session, ra);
        if (seller == null) return "redirect:/seller/renew";

        Tool tool = toolService.getToolByIdAndSeller(id, seller);
        if (tool == null) {
            ra.addFlashAttribute("error", "Tool not found.");
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
            @ModelAttribute Tool updatedTool,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "toolFile", required = false) MultipartFile toolFile,
            @RequestParam(value = "licenseDays", required = false) List<Integer> licenseDays,
            @RequestParam(value = "licensePrices", required = false) List<Double> licensePrices,
            @RequestParam(value = "action", required = false) String action,
            HttpSession session,
            RedirectAttributes ra
    ) {

        Account seller = requireActiveSeller(session, ra);
        if (seller == null) return "redirect:/seller/renew";

        Tool existing = toolService.getToolByIdAndSeller(id, seller);
        if (existing == null) {
            ra.addFlashAttribute("error", "Tool not found.");
            return "redirect:/toollist";
        }

        // TOKEN MODE → REDIRECT SANG TOKEN FLOW
        if ("token".equals(action) && existing.getLoginMethod() == Tool.LoginMethod.TOKEN) {

            toolFlowService.startEditToolSession(
                    existing.getToolId(),
                    licenseDays != null ? licenseDays : new ArrayList<>(),
                    licensePrices != null ? licensePrices : new ArrayList<>(),
                    session
            );

            ra.addFlashAttribute("info", "Update tokens before finalizing.");
            return "redirect:/tools/token/edit";
        }

        // USERNAME / PASSWORD MODE
        try {
            String imgPath = null;
            String toolPath = null;

            if (imageFile != null && !imageFile.isEmpty())
                imgPath = fileStorageService.uploadImage(imageFile);

            if (toolFile != null && !toolFile.isEmpty())
                toolPath = fileStorageService.uploadToolFile(toolFile);

            toolService.updateTool(
                    id,
                    updatedTool,
                    imgPath,
                    toolPath,
                    licenseDays != null ? licenseDays : new ArrayList<>(),
                    licensePrices != null ? licensePrices : new ArrayList<>(),
                    seller
            );

            ra.addFlashAttribute("success", "Tool updated successfully.");
            return "redirect:/toollist";

        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/tools/seller/edit/" + id;
        }
    }

    /* ===========================================================
       TOOL DETAIL
     =========================================================== */
    @GetMapping("/detail/{id}")
    public String viewToolDetail(
            @PathVariable Long id,
            Model model,
            HttpServletRequest request
    ) {

        Tool tool = toolService.getToolById(id);
        Account account = (Account) request.getSession().getAttribute("loggedInAccount");

        boolean isCustomer =
                account != null && account.getRole().getRoleName() == Role.RoleName.CUSTOMER;

        List<Feedback> feedbacks =
                feedbackRepo.findByToolAndStatus(
                        tool,
                        Feedback.Status.PUBLISHED,
                        Pageable.unpaged()
                ).getContent();

        model.addAttribute("tool", tool);
        model.addAttribute("licenses", tool.getLicenses());
        model.addAttribute("feedbacks", feedbacks);
        model.addAttribute("isCustomer", isCustomer);
        model.addAttribute("account", account);

        return "tool/tool-detail";
    }

    /* ===========================================================
       CANCEL ADD
     =========================================================== */
    @PostMapping("/seller/add/cancel")
    public String cancelAddTool(HttpSession session, RedirectAttributes ra) {
        toolFlowService.cancelToolCreation(session);
        ra.addFlashAttribute("info", "Tool creation canceled.");
        return "redirect:/toollist";
    }
}
