package swp391.fa25.lms.controller.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import swp391.fa25.lms.service.SupportService;

@Controller
@RequiredArgsConstructor
public class SupportController {

    private final SupportService supportService;

    @RequestMapping("/support")
    public String supportPage() {
        return "auth/support";
    }

    @PostMapping("/contact/send")
    public String sendSupport(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String subject,
            @RequestParam String message
    ) {
        supportService.sendSupportEmail(name, email, subject, message);
        return "redirect:/support?success";
    }
}
