package swp391.fa25.lms.config.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationFailureHandler
        implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {

        String error = "login_error";

        String message = exception.getMessage();

        if ("EMAIL_INVALID".equals(message)) {
            error = "email_invalid";
        } else if ("ACCOUNT_NOT_VERIFIED".equals(message)) {
            error = "account_not_verified";
        } else if ("ACCOUNT_DEACTIVATED".equals(message)) {
            error = "account_deactivated";
        } else if ("Email not found".equals(message)) {
            error = "account_not_found";
        }

        response.sendRedirect("/login?error=" + error);
    }
}
