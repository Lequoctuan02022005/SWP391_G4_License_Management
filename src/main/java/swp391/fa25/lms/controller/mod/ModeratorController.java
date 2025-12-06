package swp391.fa25.lms.controller.mod;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@PreAuthorize("hasRole('MOD')")
@RequestMapping("/moderator")
public class ModeratorController {

    @GetMapping("/dashboard")
    public String dashboard() {
        return "moderator/dashboard";
    }
}
