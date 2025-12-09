package swp391.fa25.lms.controller.tool;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Category;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.service.AccountService;
import swp391.fa25.lms.service.ToolListService;

import java.util.List;

@Controller
@RequestMapping("/toollist")
public class ToolListController {

    @Autowired
    private ToolListService toolListService;

    @Autowired
    private AccountService accountService;

    @GetMapping
    public String viewToolList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String loginMethod,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String priceMin,
            @RequestParam(required = false) String priceMax,
            @RequestParam(required = false) String sort,

            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "6") int size,

            Model model,
            Authentication auth) {

        // ====== FIX 1: CHUYỂN PARAM "null" THÀNH null THẬT ======

        Long categoryIdVal = parseLongOrNull(categoryId);
        Long authorVal = parseLongOrNull(author);
        Integer priceMinVal = parseIntOrNull(priceMin);
        Integer priceMaxVal = parseIntOrNull(priceMax);

        // Khi user chọn "Tất cả"
        if (categoryIdVal != null && categoryIdVal == 0) categoryIdVal = null;
        if (authorVal != null && authorVal == 0) authorVal = null;

        boolean isSeller = false;
        Long sellerId = null;

        if (auth != null) {
            Account acc = accountService.findByEmail(auth.getName());
            if (acc != null && acc.getRole().getRoleId() == 2) {
                isSeller = true;
                sellerId = acc.getAccountId();
            }
        }

        // ====== FIX 2: PARSE ENUM AN TOÀN ======
        Tool.LoginMethod loginMethodEnum = null;
        if (loginMethod != null && !loginMethod.isBlank() && !"null".equals(loginMethod)) {
            try { loginMethodEnum = Tool.LoginMethod.valueOf(loginMethod); } catch (Exception ignored) {}
        }

        Tool.Status statusEnum = null;
        if (status != null && !status.isBlank() && !"null".equals(status)) {
            try { statusEnum = Tool.Status.valueOf(status); } catch (Exception ignored) {}
        }

        // ====== LOAD DATA PHÂN TRANG ======
        List<Tool> tools;
        int totalTools;

        if (isSeller) {
            tools = toolListService.getToolsForSellerPaginated(
                    sellerId, keyword, categoryIdVal, loginMethodEnum, statusEnum,
                    priceMinVal, priceMaxVal, sort, page, size
            );
            totalTools = toolListService.countToolsForSeller(
                    sellerId, keyword, categoryIdVal, loginMethodEnum, statusEnum,
                    priceMinVal, priceMaxVal
            );
        } else {
            tools = toolListService.getToolsForUserPaginated(
                    keyword, categoryIdVal, authorVal, loginMethodEnum,
                    priceMinVal, priceMaxVal, sort, page, size
            );
            totalTools = toolListService.countToolsForUser(
                    keyword, categoryIdVal, authorVal, loginMethodEnum,
                    priceMinVal, priceMaxVal
            );
        }

        int totalPages = (int) Math.ceil((double) totalTools / size);

        // ====== FULL FILTER DATA ======
        model.addAttribute("categories", toolListService.getAllCategoriesFromDB());
        model.addAttribute("sellers", toolListService.getAllSellersFromDB());
        model.addAttribute("statuses", toolListService.getAllStatuses());
        model.addAttribute("loginMethods", toolListService.getAllLoginMethods());

        // Data
        model.addAttribute("tools", tools);
        model.addAttribute("isSeller", isSeller);

        // Giữ lại filter khi reload
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryIdVal);
        model.addAttribute("loginMethod", loginMethod);
        model.addAttribute("status", status);
        model.addAttribute("author", authorVal);
        model.addAttribute("priceMin", priceMinVal);
        model.addAttribute("priceMax", priceMaxVal);
        model.addAttribute("sort", sort);

        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("totalPages", totalPages);

        return "tool/toollist";
    }

    // ================== FIX 3: HÀM CHUYỂN "null" THÀNH null ================
    private Long parseLongOrNull(String value) {
        if (value == null || value.equals("null") || value.isBlank()) return null;
        try { return Long.valueOf(value); } catch (Exception e) { return null; }
    }

    private Integer parseIntOrNull(String value) {
        if (value == null || value.equals("null") || value.isBlank()) return null;
        try { return Integer.valueOf(value); } catch (Exception e) { return null; }
    }

    // ===========================================================
    @PostMapping("/toggle/{id}")
    public String toggleToolStatus(@PathVariable("id") Long toolId,
                                   Authentication auth) {

        if (auth == null) return "redirect:/login";

        Account acc = accountService.findByEmail(auth.getName());
        if (acc == null || acc.getRole().getRoleId() != 2) {
            return "redirect:/toollist";
        }

        toolListService.toggleStatus(toolId, acc.getAccountId());
        return "redirect:/toollist";
    }
}
