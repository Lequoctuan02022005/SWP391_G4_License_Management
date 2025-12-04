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
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.service.AccountService;
import swp391.fa25.lms.service.FileStorageService;
import swp391.fa25.lms.service.ToolFlowService;
import swp391.fa25.lms.service.ToolService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttrs
    ) {
//        Account seller = requireActiveSeller(session, redirectAttrs);
//        if (seller == null) {
//            return "redirect:/login";
//        }

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
                // hiện tại bạn vẫn dùng TokenController riêng ở /seller/token-manage
                return "redirect:/tool/token-manage";
            }

            redirectAttrs.addFlashAttribute("success", "Tool created successfully!");
            return "redirect:/tools/seller";

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
            redirectAttrs.addFlashAttribute("error", "Tool not found or unauthorized.");
            return "redirect:/tools/seller";
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
                // hiện tại vẫn dùng TokenController ở /seller/token-manage/edit
                return "redirect:/tool/token-manage/edit";
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

}
