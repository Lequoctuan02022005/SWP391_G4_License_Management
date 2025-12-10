package swp391.fa25.lms.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.repository.ProfileRepository;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;

    public Account getByEmail(String email) {
        return profileRepository.findByEmail(email);
    }

    public Account getById(Long id) {
        return profileRepository.findById(id).orElse(null);
    }

    public void save(Account account) {
        profileRepository.save(account);
    }
}
