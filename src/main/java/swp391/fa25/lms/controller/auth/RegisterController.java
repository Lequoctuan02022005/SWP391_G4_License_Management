package swp391.fa25.lms.controller.auth;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.service.AccountService;

@Controller
public class RegisterController {

    @Autowired
    private AccountService accountService;

    // Hiển thị form đăng ký
    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        if (!model.containsAttribute("account")) {
            model.addAttribute("account", new Account());
        }
        return "auth/register";
    }

    // Xử lý đăng ký
    @PostMapping("/register")
    public String handleRegister(@Valid @ModelAttribute("account") Account account,
                                 BindingResult result,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {

        //  Lỗi xác nhận mật khẩu
        if (account.getConfirmPassword() == null ||
                !account.getPassword().equals(account.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "error.confirmPassword", "Mật khẩu không khớp.");
        }

        boolean success = accountService.registerAccount(account, result);

        //  Nếu có lỗi ở lại trang đăng ký
        if (result.hasErrors() || !success) {
            model.addAttribute("showAlert", true);
            model.addAttribute("alertType", "danger");
            model.addAttribute("alertMessage", "Vui lòng sửa các lỗi bên dưới và thử lại.");
            return "auth/register";
        }

        // ✅ Nếu đăng ký OK:
        // - Account đã được lưu với status = DEACTIVATED, verified = false
        // - Mã verify đã được gửi email
        // → Chuyển sang trang /verify để nhập mã
        redirectAttributes.addFlashAttribute(
                "infoMessage",
                "Đăng ký thành công! Vui lòng kiểm tra email để lấy mã xác minh."
        );
        return "redirect:/verify";
    }

    // Hiển thị form nhập mã code
    @GetMapping("/verify")
    public String showVerifyPage(Model model) {
        // infoMessage có thể được set từ redirectAttributes
        return "auth/verify-code";
    }

    // Xử lý mã code
    @PostMapping("/verify")
    public String verifyCode(@RequestParam("code") String code,
                             Model model) {
        try {
            accountService.verifyCode(code);
            model.addAttribute("successMessage", "Xác minh thành công! Cảm ơn bạn đã đăng kí! Xin mời bạn đăng nhập lại sau 4 giây.");
            model.addAttribute("redirectUrl", "/login");  // verify-code.html sẽ tự redirect sau 2s
        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", e.getMessage());
        }
        return "auth/verify-code";
    }

}
