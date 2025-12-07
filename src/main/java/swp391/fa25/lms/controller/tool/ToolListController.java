package swp391.fa25.lms.controller.tool;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.service.ToolListService;
import swp391.fa25.lms.service.AccountService;

import java.util.List;

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
                               @RequestParam(required = false) String loginMethod,
                               @RequestParam(required = false) String status,
                               Model model,
                               Authentication auth) {

        boolean isSeller = false;
        Long sellerId = null;

        if (auth != null) {

            // ✨ Lấy email đăng nhập
            String email = auth.getName();

            // ✨ Lấy account từ DB
            Account acc = accountService.findByEmail(email);


            if (acc != null) {

                // Kiểm tra role seller (role_id = 2)
                isSeller = acc.getRole().getRoleId() == 2;

                if (isSeller) {
                    sellerId = acc.getAccountId();
                }
            }
        }

        // Convert enums safely
        Tool.LoginMethod loginMethodEnum = null;
        if (loginMethod != null && !loginMethod.isBlank()) {
            try {
                loginMethodEnum = Tool.LoginMethod.valueOf(loginMethod);
            } catch (Exception ignore) {
            }
        }

        Tool.Status statusEnum = null;
        if (status != null && !status.isBlank()) {
            try {
                statusEnum = Tool.Status.valueOf(status);
            } catch (Exception ignore) {
            }
        }

        List<Tool> tools;

        if (isSeller) {
            tools = toolListService.getToolsForSeller(sellerId, keyword, loginMethodEnum, statusEnum);
        } else {
            tools = toolListService.getToolsForUser(keyword, loginMethodEnum);
        }

        model.addAttribute("tools", tools);
        model.addAttribute("isSeller", isSeller);

        model.addAttribute("keyword", keyword);
        model.addAttribute("loginMethod", loginMethod);
        model.addAttribute("status", status);

        return "tool/toollist";
    }

    // =============== TOGGLE STATUS ===============
    @PostMapping("/toggle/{id}")
    public String toggleToolStatus(@PathVariable("id") Long toolId,
                                   Authentication auth) {

        if (auth == null) return "redirect:/login";

        String email = auth.getName();
        Account acc = accountService.findByEmail(email);

        if (acc == null || acc.getRole().getRoleId() != 2) {
            return "redirect:/toollist"; // Không phải seller thì không được toggle
        }

        Long sellerId = acc.getAccountId();
        toolListService.toggleStatus(toolId, sellerId);

        return "redirect:/toollist";
    }
}