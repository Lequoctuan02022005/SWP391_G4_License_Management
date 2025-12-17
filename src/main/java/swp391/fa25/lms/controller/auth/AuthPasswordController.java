package swp391.fa25.lms.controller.auth;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import swp391.fa25.lms.service.AccountService;
import swp391.fa25.lms.model.Account;

@Controller
public class AuthPasswordController {

    private final AccountService accountService;

    public AuthPasswordController(AccountService accountService) {
        this.accountService = accountService;
    }

    // ========== FORGOT PASSWORD ==========

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm(Model model) {
        if (!model.containsAttribute("email")) {
            model.addAttribute("email", "");
        }
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(@RequestParam("email") String email,
                                       HttpSession session,
                                       RedirectAttributes redirectAttributes) {
        try {
            accountService.initiateForgotPassword(email);

            // Lưu email vào session để dùng cho verify-reset và reset-password
            session.setAttribute("resetEmail", email);
            session.setAttribute("resetVerified", false);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Mã xác minh đặt lại mật khẩu đã được gửi tới email của bạn.");
            return "redirect:/verify-reset";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("email", email);
            return "redirect:/forgot-password";
        }
    }

    // ========== VERIFY RESET CODE ==========

    @GetMapping("/verify-reset")
    public String showVerifyResetForm(HttpSession session, Model model) {
        String email = (String) session.getAttribute("resetEmail");
        if (email == null) {
            model.addAttribute("errorMessage", "Phiên khôi phục mật khẩu đã hết hạn. Vui lòng nhập lại email.");
            return "auth/forgot-password";
        }
        return "auth/verify-reset";
    }

    @PostMapping("/verify-reset")
    public String handleVerifyReset(@RequestParam("code") String code,
                                    HttpSession session,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {

        String email = (String) session.getAttribute("resetEmail");
        if (email == null) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Phiên khôi phục mật khẩu đã hết hạn. Vui lòng thực hiện lại.");
            return "redirect:/forgot-password";
        }

        try {
            accountService.verifyPasswordResetCode(email, code);

            // Đánh dấu là đã verify code thành công
            session.setAttribute("resetVerified", true);

            return "redirect:/reset-password";
        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/verify-reset";
        }
    }

    // ========== RESET PASSWORD ==========

    @GetMapping("/reset-password")
    public String showResetPasswordForm(HttpSession session, Model model) {
        String email = (String) session.getAttribute("resetEmail");
        Boolean resetVerified = (Boolean) session.getAttribute("resetVerified");

        if (email == null || resetVerified == null || !resetVerified) {
            model.addAttribute("errorMessage",
                    "Bạn cần xác minh mã trước khi đặt lại mật khẩu.");
            return "auth/forgot-password";
        }

        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String handleResetPassword(@RequestParam("newPassword") String newPassword,
                                      @RequestParam("confirmPassword") String confirmPassword,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes,
                                      Model model) {

        String email = (String) session.getAttribute("resetEmail");
        Boolean resetVerified = (Boolean) session.getAttribute("resetVerified");

        if (email == null || resetVerified == null || !resetVerified) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Phiên đặt lại mật khẩu đã hết hạn. Vui lòng thực hiện lại.");
            return "redirect:/forgot-password";
        }

        try {
            accountService.resetPasswordAfterVerify(email, newPassword, confirmPassword);

            // Xóa dữ liệu trong session
            session.removeAttribute("resetEmail");
            session.removeAttribute("resetVerified");

            redirectAttributes.addFlashAttribute("successMessage",
                    "Đặt lại mật khẩu thành công. Vui lòng đăng nhập.");
            return "redirect:/login";
        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/reset-password";
        }
    }

    // ========== CHANGE PASSWORD (KHI ĐÃ ĐĂNG NHẬP) ==========

    @GetMapping("/change-password")
    public String showChangePasswordForm() {
        return "auth/change-password";
    }

    @PostMapping("/change-password")
    public String handleChangePassword(@RequestParam("oldPassword") String oldPassword,
                                       @RequestParam("newPassword") String newPassword,
                                       @RequestParam("confirmPassword") String confirmPassword,
                                       HttpSession session,
                                       RedirectAttributes redirectAttributes) {

        // Lấy email từ session. Most controllers save the Account object
        // under "loggedInAccount". Fall back to "loggedInAccountEmail" if present.
        Account logged = (Account) session.getAttribute("loggedInAccount");
        String email = null;
        if (logged != null) {
            email = logged.getEmail();
        } else {
            email = (String) session.getAttribute("loggedInAccountEmail");
        }

        if (email == null) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Bạn cần đăng nhập để đổi mật khẩu.");
            return "redirect:/login";
        }

        try {
            accountService.changePassword(email, oldPassword, newPassword, confirmPassword);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đổi mật khẩu thành công.");
            return "redirect:/change-password";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/change-password";
        }
    }
}
