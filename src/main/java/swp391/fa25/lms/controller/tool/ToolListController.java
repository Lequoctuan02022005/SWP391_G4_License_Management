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
import java.util.Objects;

@Controller
@RequestMapping("/toollist")
public class ToolListController {

    @Autowired
    private ToolListService toolListService;

    @Autowired
    private AccountService accountService;

    // =============== VIEW LIST ===============
    @GetMapping
    public String viewToolList(@RequestParam(required = false) String keyword,
                               @RequestParam(required = false) Long categoryId,
                               @RequestParam(required = false) String loginMethod,
                               @RequestParam(required = false) String status,
                               @RequestParam(required = false) Long author,
                               @RequestParam(required = false) Integer priceMin,
                               @RequestParam(required = false) Integer priceMax,
                               @RequestParam(required = false) String sort,
                               Model model,
                               Authentication auth) {

        boolean isSeller = false;
        Long sellerId = null;

        if (auth != null) {
            Account acc = accountService.findByEmail(auth.getName());
            if (acc != null && acc.getRole().getRoleId() == 2) { // role 2 = seller
                isSeller = true;
                sellerId = acc.getAccountId();
            }
        }

        Tool.LoginMethod loginMethodEnum = null;
        if (loginMethod != null && !loginMethod.isBlank()) {
            try {
                loginMethodEnum = Tool.LoginMethod.valueOf(loginMethod);
            } catch (Exception ignore) {}
        }

        Tool.Status statusEnum = null;
        if (status != null && !status.isBlank()) {
            try {
                statusEnum = Tool.Status.valueOf(status);
            } catch (Exception ignore) {}
        }

        List<Tool> tools;

        if (isSeller) {
            // seller: lọc theo status, KHÔNG lọc theo author (vì đã gắn sellerId)
            tools = toolListService.getToolsForSeller(
                    sellerId,
                    keyword,
                    categoryId,
                    loginMethodEnum,
                    statusEnum,
                    priceMin,
                    priceMax,
                    sort
            );
        } else {
            // user: không lọc status, nhưng lọc theo author
            tools = toolListService.getToolsForUser(
                    keyword,
                    categoryId,
                    author,
                    loginMethodEnum,
                    priceMin,
                    priceMax,
                    sort
            );
        }

        // Dynamic filter lists dựa trên list tool đang có
        List<Category> categories = toolListService.getAvailableCategories(tools);
        List<Account> sellers = toolListService.getAvailableSellers(tools);
        List<Tool.Status> statuses = toolListService.getAvailableStatuses(tools);
        List<Tool.LoginMethod> loginMethods = toolListService.getAvailableLoginMethods(tools);

        model.addAttribute("tools", tools);
        model.addAttribute("isSeller", isSeller);

        model.addAttribute("categories", categories);
        model.addAttribute("sellers", sellers);
        model.addAttribute("statuses", statuses);
        model.addAttribute("loginMethods", loginMethods);

        // giữ lại giá trị filter trên UI
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("loginMethod", loginMethod);
        model.addAttribute("status", status);
        model.addAttribute("author", author);
        model.addAttribute("priceMin", priceMin);
        model.addAttribute("priceMax", priceMax);
        model.addAttribute("sort", sort);

        return "tool/toollist";
    }

    // =============== TOGGLE STATUS ===============
    @PostMapping("/toggle/{id}")
    public String toggleToolStatus(@PathVariable("id") Long toolId,
                                   Authentication auth) {

        if (auth == null) return "redirect:/login";

        Account acc = accountService.findByEmail(auth.getName());
        if (acc == null || acc.getRole().getRoleId() != 2) {
            // Không phải seller thì không được toggle
            return "redirect:/toollist";
        }

        toolListService.toggleStatus(toolId, acc.getAccountId());
        return "redirect:/toollist";
    }

    // =============== ADD TOOL ===============
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("tool", new Tool());
        return "tool/toollist-add";
    }

    @PostMapping("/new")
    public String createTool(@ModelAttribute Tool tool,
                             Authentication auth) {

        if (auth == null) return "redirect:/login";

        Account acc = accountService.findByEmail(auth.getName());
        if (acc == null || acc.getRole().getRoleId() != 2) {
            return "redirect:/toollist";
        }

        toolListService.addTool(tool, acc.getAccountId());
        return "redirect:/toollist";
    }

    // =============== EDIT TOOL (VIEW FORM) ===============
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long toolId,
                               Model model,
                               Authentication auth) {

        if (auth == null) return "redirect:/login";

        Account acc = accountService.findByEmail(auth.getName());
        if (acc == null || acc.getRole().getRoleId() != 2) {
            return "redirect:/toollist";
        }

        // Lấy tool thuộc chính seller
        Tool tool = toolListService.getToolsForSeller(
                        acc.getAccountId(),
                        null, null,
                        null, null,
                        null, null,
                        null)
                .stream()
                .filter(t -> Objects.equals(t.getToolId(), toolId))
                .findFirst()
                .orElse(null);

        model.addAttribute("tool", tool);
        return "tool/toollist-edit";
    }
}
