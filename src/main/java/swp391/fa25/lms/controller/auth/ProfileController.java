package swp391.fa25.lms.controller.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.service.ProfileService;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
@RequestMapping("/profile")
public class ProfileController {

    private final ProfileService profileService;
    private final PasswordEncoder passwordEncoder;

    // ====================== VIEW PROFILE ======================
    @GetMapping
    public String viewProfile(Authentication auth, Model model) {
        Account acc = profileService.getByEmail(auth.getName());
        model.addAttribute("profile", acc);
        return "common/profile";
    }

    // ====================== UPDATE INFO ======================
    @PostMapping("/update")
    public String updateProfile(Authentication auth,
                                @RequestParam String fullName,
                                @RequestParam String phone,
                                @RequestParam String address,
                                RedirectAttributes redirectAttributes) {

        // Server-side validation:
        // phone: empty or starts with 09/03 and 10 digits
        String phonePattern = "^(09|03)\\d{8}$";
        if (phone != null && !phone.isBlank() && !phone.matches(phonePattern)) {
            redirectAttributes.addFlashAttribute("updateError", "Số điện thoại phải là 10 chữ số và bắt đầu bằng 09 hoặc 03");
            redirectAttributes.addFlashAttribute("openUpdateModal", true);
            return "redirect:/profile";
        }

        // fullName: required, 3-100 letters (allow unicode letters, spaces and basic punctuation)
        String fullNamePattern = "^[\\p{L} .'-]{3,100}$";
        if (fullName == null || fullName.isBlank() || !fullName.matches(fullNamePattern)) {
            redirectAttributes.addFlashAttribute("updateError", "Họ tên không hợp lệ (5-100 ký tự, chỉ chữ và khoảng trắng)");
            redirectAttributes.addFlashAttribute("openUpdateModal", true);
            return "redirect:/profile";
        }

        // address: optional but if provided should be reasonable length
        if (address != null && !address.isBlank()) {
            if (address.length() < 5 || address.length() > 200) {
                redirectAttributes.addFlashAttribute("updateError", "Địa chỉ không hợp lệ (5-200 ký tự)");
                redirectAttributes.addFlashAttribute("openUpdateModal", true);
                return "redirect:/profile";
            }
        }

        Account acc = profileService.getByEmail(auth.getName());

        acc.setFullName(fullName);
        acc.setPhone(phone);
        acc.setAddress(address);
        acc.setUpdatedAt(LocalDateTime.now());

        profileService.save(acc);

        return "redirect:/profile?updated=true";
    }


    // ====================== UPLOAD AVATAR ======================
    @PostMapping("/update-avatar")
    public String uploadAvatar(Authentication auth,
                               @RequestParam("avatar") MultipartFile file) throws IOException {

        Account acc = profileService.getByEmail(auth.getName());

        if (!file.isEmpty()) {

            // Folder uploads/avatar nằm trong project (gốc chạy ứng dụng)
            String uploadDirPath = System.getProperty("user.dir") + "/uploads/avatar/";
            File uploadDir = new File(uploadDirPath);
            if (!uploadDir.exists()) uploadDir.mkdirs();

            // Xóa avatar cũ nếu có
            if (acc.getImages() != null && !acc.getImages().isBlank()) {
                File oldFile = new File(uploadDir, acc.getImages());
                if (oldFile.exists()) oldFile.delete();
            }

            // Lưu file mới
            String originalFilename = file.getOriginalFilename();
            String ext = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                ext = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String fileName = "AVT_" + acc.getAccountId() + "_" + System.currentTimeMillis() + ext;
            File dest = new File(uploadDir, fileName);
            file.transferTo(dest);

            // Cập nhật database
            acc.setImages(fileName);
            acc.setUpdatedAt(LocalDateTime.now());
            profileService.save(acc);
        }

        return "redirect:/profile?avatarUpdated=true";
    }

}
