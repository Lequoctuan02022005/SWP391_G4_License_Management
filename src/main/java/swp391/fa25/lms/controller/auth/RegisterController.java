package swp391.fa25.lms.controller.auth;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.service.AccountService;

@Controller
@RequiredArgsConstructor
public class RegisterController {

    private static final Logger logger = LoggerFactory.getLogger(RegisterController.class);
    private final AccountService accountService;

    // ================== REGISTER ==================
    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("account", new Account());
        return "auth/register";
    }

    @PostMapping("/register")
    public String handleRegister(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String password,
            @RequestParam(required = false) String confirmPassword,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            Account account = bindAccountFromParams(email, fullName, phone, address, password, confirmPassword);
            BindingResult result = new BeanPropertyBindingResult(account, "account");

            validateAccount(account, result);

            if (result.hasErrors()) {
                model.addAttribute("account", account);
                return "auth/register";
            }

            boolean success = accountService.registerAccount(account, result);

            if (!success || result.hasErrors()) {
                model.addAttribute("account", account);
                return "auth/register";
            }

            redirectAttributes.addFlashAttribute(
                    "infoMessage",
                    "Đăng ký thành công! Vui lòng kiểm tra email để lấy mã xác minh."
            );
            return "redirect:/verify";

        } catch (Exception e) {
            logger.error("Error in handleRegister", e);
            Account account = new Account();
            model.addAttribute("account", account);
            model.addAttribute("showAlert", true);
            model.addAttribute("alertType", "danger");
            model.addAttribute("alertMessage", "Đã xảy ra lỗi hệ thống: " + e.getMessage());
            return "auth/register";
        }
    }

    // ================== VERIFY ==================
    @GetMapping("/verify")public String showVerifyPage() {
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

    private Account bindAccountFromParams(String email, String fullName, String phone,
                                          String address, String password, String confirmPassword) {
        Account account = new Account();
        if (email != null) account.setEmail(email);
        if (fullName != null) account.setFullName(fullName);
        if (phone != null) account.setPhone(phone);
        if (address != null) account.setAddress(address);
        if (password != null) account.setPassword(password);
        if (confirmPassword != null) account.setConfirmPassword(confirmPassword);
        return account;
    }

    private void validateAccount(Account account, BindingResult result) {
        // Validate email
        if (account.getEmail() == null || account.getEmail().trim().isEmpty()) {
            result.rejectValue("email", "error.email", "Email không được để trống.");
        } else if (!account.getEmail().trim().matches("^[a-zA-Z0-9._%+-]+@gmail\\.com$")) {
            result.rejectValue("email", "error.email", "Email phải có đuôi @gmail.com");
        }

        // Validate fullName
        if (account.getFullName() == null || account.getFullName().trim().isEmpty()) {
            result.rejectValue("fullName", "error.fullName", "Họ và tên không được để trống.");
        } else {
            String trimmedName = account.getFullName().trim();
            if (trimmedName.length() < 5 || trimmedName.length() > 20) {
                result.rejectValue("fullName", "error.fullName", "Họ và tên đầy đủ phải từ 5 đến 20 ký tự.");
            }
        }

        // Validate phone (optional nhưng nếu có thì phải đúng format)
        if (account.getPhone() != null && !account.getPhone().trim().isEmpty()) {
            if (!account.getPhone().trim().matches("0\\d{9}")) {
                result.rejectValue("phone", "error.phone", "Số điện thoại phải có 10 chữ số bắt đầu bằng số 0.");
            }
        }

        // Validate password
        if (account.getPassword() == null || account.getPassword().trim().isEmpty()) {
            result.rejectValue("password", "error.password", "Mật khẩu không được để trống.");
        }if (account.getPassword() != null && !account.getPassword().trim().isEmpty()) {
            if (account.getConfirmPassword() == null || account.getConfirmPassword().trim().isEmpty()) {
                result.rejectValue("confirmPassword", "error.confirmPassword", "Vui lòng xác nhận mật khẩu.");
            } else if (!account.getPassword().equals(account.getConfirmPassword())) {
                result.rejectValue("confirmPassword", "error.confirmPassword", "Mật khẩu không khớp.");
            }
        }
    }
}

        // Check confirm password