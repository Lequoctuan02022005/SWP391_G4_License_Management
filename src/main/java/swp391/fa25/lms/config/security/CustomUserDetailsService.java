package swp391.fa25.lms.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.repository.AccountRepository;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AccountRepository accountRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 1. Validate email format
        if (email == null || !email.endsWith("@gmail.com")) {
            throw new UsernameNotFoundException("EMAIL_INVALID");
        }

        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Email not found"));

        // 2. Check status
        if (account.getStatus() == Account.AccountStatus.DEACTIVATED) {
            throw new UsernameNotFoundException("ACCOUNT_DEACTIVATED");
        }

        // 3. Check verified
        if (account.getVerified() == null || !account.getVerified()) {
            throw new UsernameNotFoundException("ACCOUNT_NOT_VERIFIED");
        }
        return new CustomUserDetails(account);
    }
}
