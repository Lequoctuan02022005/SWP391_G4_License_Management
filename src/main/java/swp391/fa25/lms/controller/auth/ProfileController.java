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

    private static final String[] ALLOWED_IMAGE_EXTENSIONS = {
            ".jpg", ".jpeg", ".png", ".gif", ".webp"
    };

    private final ProfileService profileService;
    private final PasswordEncoder passwordEncoder;

    // ====================== VIEW PROFILE ======================
    @GetMapping
    public String viewProfile(Authentication auth, Model model) {
        Account acc = profileService.getByEmail(auth.getName());
        model.addAttribute("profile", acc); // CHỈ DÙNG profile
        return "common/profile";
    }

    // ====================== UPDATE INFO ======================
    @PostMapping("/update")
    public String updateProfile(Authentication auth,
                                @RequestParam String fullName,
                                @RequestParam String phone,
                                @RequestParam String address,
                                RedirectAttributes redirectAttributes) {

        // Phone validation
        String phonePattern = "^(09|03)\\d{8}$";
        if (phone != null && !phone.isBlank() && !phone.matches(phonePattern)) {
            redirectAttributes.addFlashAttribute("updateError",
                    "Số điện thoại phải có 10 chữ số và bắt đầu bằng 09 hoặc 03.");
            redirectAttributes.addFlashAttribute("openUpdateModal", true);
            return "redirect:/profile";
        }

        // Full name validation
        String fullNamePattern = "^[\\p{L} .'-]{5,100}$";
        if (fullName == null || fullName.isBlank() || !fullName.matches(fullNamePattern)) {
            redirectAttributes.addFlashAttribute("updateError",
                    "Họ tên không hợp lệ (5–100 ký tự, chỉ chữ).");
            redirectAttributes.addFlashAttribute("openUpdateModal", true);
            return "redirect:/profile";
        }

        // Address validation
        if (address != null && !address.isBlank()) {
            if (address.length() < 5 || address.length() > 200) {
                redirectAttributes.addFlashAttribute("updateError",
                        "Địa chỉ phải từ 5 đến 200 ký tự.");
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
                               @RequestParam("avatar") MultipartFile file,
                               RedirectAttributes redirectAttributes) {

        Account acc = profileService.getByEmail(auth.getName());

        if (file == null || file.isEmpty()) {
            redirectAttributes.addFlashAttribute("avatarError",
                    "Vui lòng chọn một file ảnh.");
            redirectAttributes.addFlashAttribute("openAvatarModal", true);
            return "redirect:/profile";
        }

        String filename = file.getOriginalFilename();
        String ext = "";

        if (filename != null && filename.contains(".")) {
            ext = filename.substring(filename.lastIndexOf(".")).toLowerCase();
        }

        boolean valid = false;
        for (String allow : ALLOWED_IMAGE_EXTENSIONS) {
            if (allow.equals(ext)) {
                valid = true;
                break;
            }
        }

        if (!valid) {
            redirectAttributes.addFlashAttribute("avatarError",
                    "Chỉ được upload ảnh (jpg, jpeg, png, gif, webp).");
            redirectAttributes.addFlashAttribute("openAvatarModal", true);
            return "redirect:/profile";
        }

        try {
            String uploadDirPath = System.getProperty("user.dir") + "/uploads/avatar/";
            File uploadDir = new File(uploadDirPath);
            if (!uploadDir.exists()) uploadDir.mkdirs();

            if (acc.getImages() != null && !acc.getImages().isBlank()) {
                File oldFile = new File(uploadDir, acc.getImages());
                if (oldFile.exists()) oldFile.delete();
            }

            String newFile = "AVT_" + acc.getAccountId() + "_" + System.currentTimeMillis() + ext;
            file.transferTo(new File(uploadDir, newFile));

            acc.setImages(newFile);
            acc.setUpdatedAt(LocalDateTime.now());
            profileService.save(acc);

        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("avatarError",
                    "Lỗi upload ảnh. Vui lòng thử lại.");
            redirectAttributes.addFlashAttribute("openAvatarModal", true);
            return "redirect:/profile";
        }

        return "redirect:/profile?avatarUpdated=true";
    }
}
