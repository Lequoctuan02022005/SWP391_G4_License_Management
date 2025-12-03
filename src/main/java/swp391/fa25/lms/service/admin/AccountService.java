package swp391.fa25.lms.service.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.repository.admin.AccountRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    public Page<Account> getAll(Pageable pageable) {
        return accountRepository.findAll(pageable);
    }

    public Account getById(Long id) {
        return accountRepository.findById(id).orElse(null);
    }

    public Page<Account> search(String keyword, Pageable pageable) {
        return accountRepository.searchPage(keyword, pageable);
    }


    public Account create(Account account) {

        if (accountRepository.existsByEmail(account.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());

        if (account.getStatus() == null) {
            account.setStatus(Account.AccountStatus.ACTIVE);
        }

        return accountRepository.save(account);
    }

    public Account update(Long id, Account updated) {
        Account acc = accountRepository.findById(id).orElse(null);
        if (acc == null) return null;

        acc.setEmail(updated.getEmail());
        acc.setFullName(updated.getFullName());
        acc.setPhone(updated.getPhone());
        acc.setAddress(updated.getAddress());
        acc.setRole(updated.getRole());
        acc.setStatus(updated.getStatus());
        acc.setUpdatedAt(LocalDateTime.now());

        if (updated.getPassword() != null && !updated.getPassword().isBlank()) {
            acc.setPassword(updated.getPassword());
        }

        return accountRepository.save(acc);
    }

    public void delete(Long id) {
        accountRepository.deleteById(id);
    }

    public boolean emailExists(String email) {
        return accountRepository.existsByEmail(email);
    }
}
