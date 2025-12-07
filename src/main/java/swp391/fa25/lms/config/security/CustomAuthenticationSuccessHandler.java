package swp391.fa25.lms.config.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Role;
import swp391.fa25.lms.model.SellerSubscription;
import swp391.fa25.lms.repository.AccountRepository;
import swp391.fa25.lms.repository.SellerSubscriptionRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class CustomAuthenticationSuccessHandler
        implements org.springframework.security.web.authentication.AuthenticationSuccessHandler {

    private final AccountRepository accountRepository;
    private final SellerSubscriptionRepository subscriptionRepository;

    public CustomAuthenticationSuccessHandler(AccountRepository accountRepository,
                                              SellerSubscriptionRepository subscriptionRepository) {
        this.accountRepository = accountRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Optional<Account> optionalAccount = accountRepository.findByEmail(userDetails.getUsername());

        if (optionalAccount.isEmpty()) {
            response.sendRedirect("/login?error");
            return;
        }

        Account account = optionalAccount.get();
        Role.RoleName role = account.getRole().getRoleName();

        // LƯU ACCOUNT VÀO SESSION
        request.getSession().setAttribute("loggedInAccount", account);

        if (role != Role.RoleName.SELLER) {
            redirectByRole(role, response);
            return;
        }

        SellerSubscription activeSub = subscriptionRepository.findByAccountOrderByStartDateDesc(account)
                .stream()
                .filter(SellerSubscription::isActive)
                .filter(sub -> sub.getEndDate().isAfter(LocalDateTime.now()))
                .findFirst()
                .orElse(null);

        if (activeSub == null) {
            response.sendRedirect("/seller/renew");
            return;
        }

        response.sendRedirect("/seller/tools");
    }

    private void redirectByRole(Role.RoleName role, HttpServletResponse response) throws IOException {
        switch (role) {
            case ADMIN -> response.sendRedirect("/admin/dashboard");
            case MANAGER -> response.sendRedirect("/manager/blogs");
            case MOD -> response.sendRedirect("/moderator/dashboard");
            case CUSTOMER -> response.sendRedirect("/home");
            default -> response.sendRedirect("/");
        }
    }
}
