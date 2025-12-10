package swp391.fa25.lms.controller.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.service.ProfileService;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
@RequestMapping("/profile")
public class ProfileController {

    private final ProfileService profileService;

    // ====================== VIEW PROFILE ======================
    @GetMapping
    public String viewProfile(Authentication auth, Model model) {
        Account acc = profileService.getByEmail(auth.getName());
        model.addAttribute("profile", acc);
        return "common/profile";   // templates/common/profile.html
    }

    // ====================== UPDATE INFO ======================
    @PostMapping("/update")
    public String updateProfile(Authentication auth,
                                @RequestParam String fullName,
                                @RequestParam String phone,
                                @RequestParam String address) {

        Account acc = profileService.getByEmail(auth.getName());

        acc.setFullName(fullName);
        acc.setPhone(phone);
        acc.setAddress(address);
        acc.setUpdatedAt(LocalDateTime.now());

        profileService.save(acc);

        return "redirect:/profile?updated=true";
    }

    // 🔥 ĐỔI MẬT KHẨU: dùng trang / controller bạn đã có, KHÔNG xử lý ở đây nữa
}
