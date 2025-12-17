package swp391.fa25.lms.controller.auth;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.service.AccountService;

@Controller
public class RegisterController {

    @Autowired
    private AccountService accountService;

    // ================== REGISTER ==================
    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("account", new Account());
        return "auth/register";
    }

    @PostMapping("/register")
    public String handleRegister(
            @Valid @ModelAttribute("account") Account account,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        // Check confirm password
        if (account.getConfirmPassword() == null ||
                !account.getPassword().equals(account.getConfirmPassword())) {
            result.rejectValue(
                    "confirmPassword",
                    "error.confirmPassword",
                    "Mật khẩu không khớp."
            );
        }

        boolean success = accountService.registerAccount(account, result);

        if (result.hasErrors() || !success) {
            model.addAttribute("showAlert", true);
            model.addAttribute("alertType", "danger");
            model.addAttribute("alertMessage", "Vui lòng sửa các lỗi bên dưới và thử lại.");
            return "auth/register";
        }

        redirectAttributes.addFlashAttribute(
                "infoMessage",
                "Đăng ký thành công! Vui lòng kiểm tra email để lấy mã xác minh."
        );
        return "redirect:/verify";
    }

    // ================== VERIFY ==================
    @GetMapping("/verify")
    public String showVerifyPage() {
            return "auth/verify-code";
    }

    @PostMapping("/verify")
    public String verifyCode(@RequestParam("code") String code, Model model) {
        try {
            accountService.verifyCode(code);
            model.addAttribute(
                    "successMessage",
                    "Xác minh thành công! Bạn sẽ được chuyển đến trang đăng nhập sau 4 giây."
            );
            model.addAttribute("redirectUrl", "/login");
        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", e.getMessage());
        }
        return "auth/verify-code";
    }
}
