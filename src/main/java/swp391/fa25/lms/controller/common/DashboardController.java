package swp391.fa25.lms.controller.common;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import swp391.fa25.lms.config.security.CustomUserDetails;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.service.DashboardService;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/dashboard")
    public String dashboard(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model
    ) {

        // ✅ FIX DUY NHẤT: tránh NPE khi session hết hạn / chưa login
        if (userDetails == null) {
            return "redirect:/login";
        }

        Account account = userDetails.getAccount();

        Map<String, Object> dashboardData =
                dashboardService.getDashboardData(account);

        model.addAllAttributes(dashboardData);

        return "common/dashboard";
    }
}
