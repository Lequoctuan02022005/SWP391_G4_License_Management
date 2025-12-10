package swp391.fa25.lms.config;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.service.ProfileService;
import lombok.RequiredArgsConstructor;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {

    private final ProfileService profileService;

    @ModelAttribute("account")
    public Account addAccountToModel(Authentication authentication) {
        if (authentication != null) {
            return profileService.getByEmail(authentication.getName());
        }
        return null;
    }

}
