package swp391.fa25.lms.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Role;
import swp391.fa25.lms.repository.AccountRepository;
import swp391.fa25.lms.repository.RoleRepository;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class RoleDataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepo;
    private final AccountRepository accountRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        System.out.println("🔹 Initializing default roles & accounts...");

        // ===== CREATE ROLES IF NOT EXIST =====
        createRoleIfMissing(1, Role.RoleName.ADMIN, "System administrator");
        createRoleIfMissing(2, Role.RoleName.SELLER, "Seller role");
        createRoleIfMissing(3, Role.RoleName.CUSTOMER, "Normal customer");
        createRoleIfMissing(4, Role.RoleName.MOD, "Moderator");
        createRoleIfMissing(5, Role.RoleName.MANAGER, "Manager");
        createRoleIfMissing(6, Role.RoleName.GUEST, "Guest user");

        // ===== CREATE ADMIN USER =====
        if (!accountRepo.existsByEmail("admin@gmail.com")) {
            Account admin = new Account();
            admin.setEmail("admin@gmail.com");
            admin.setFullName("System Admin");
            admin.setPassword(passwordEncoder.encode("123456"));
            admin.setStatus(Account.AccountStatus.ACTIVE);
            admin.setRole(roleRepo.findByRoleName(Role.RoleName.ADMIN).get());
            accountRepo.save(admin);
            System.out.println("⭐ Admin created");
        }

        // ===== CREATE SELLER USER =====
        if (!accountRepo.existsByEmail("seller@gmail.com")) {
            Account seller = new Account();
            seller.setEmail("seller@gmail.com");
            seller.setFullName("Test Seller");
            seller.setPassword(passwordEncoder.encode("123456"));
            seller.setStatus(Account.AccountStatus.ACTIVE);
            seller.setRole(roleRepo.findByRoleName(Role.RoleName.SELLER).get());
            accountRepo.save(seller);
            System.out.println("⭐ Seller created");
        }

        // ===== CREATE NORMAL CUSTOMER =====
        if (!accountRepo.existsByEmail("user@gmail.com")) {
            Account user = new Account();
            user.setEmail("user@gmail.com");
            user.setFullName("Test User");
            user.setPassword(passwordEncoder.encode("123456"));
            user.setStatus(Account.AccountStatus.ACTIVE);
            user.setRole(roleRepo.findByRoleName(Role.RoleName.CUSTOMER).get());
            accountRepo.save(user);
            System.out.println("⭐ User created");
        }

        // ===== CREATE MANAGER USER =====
        if (!accountRepo.existsByEmail("manager@gmail.com")) {
            Account manager = new Account();
            manager.setEmail("manager@gmail.com");
            manager.setFullName("Test Manager");
            manager.setPassword(passwordEncoder.encode("123456"));
            manager.setStatus(Account.AccountStatus.ACTIVE);
            manager.setRole(roleRepo.findByRoleName(Role.RoleName.MANAGER).get());
            accountRepo.save(manager);
            System.out.println("⭐ Manager created");
        }
      
            // ===== CREATE MODERATOR USER =====
            if (!accountRepo.existsByEmail("mod@gmail.com")) {
                Account mod = new Account();
                mod.setEmail("mod@gmail.com");
                mod.setFullName("Moderator User");
                mod.setPassword(passwordEncoder.encode("123456"));
                mod.setStatus(Account.AccountStatus.ACTIVE);
                mod.setRole(roleRepo.findByRoleName(Role.RoleName.MOD).get());
                accountRepo.save(mod);
                System.out.println("⭐ Moderator created");
            }
            if (!accountRepo.existsByEmail("expiredseller@gmail.com")) {

                Account expired = new Account();
                expired.setEmail("expiredseller@gmail.com");
                expired.setFullName("Seller Hết Hạn");
                expired.setPassword(passwordEncoder.encode("123456"));
                expired.setStatus(Account.AccountStatus.ACTIVE);
                expired.setRole(roleRepo.findByRoleName(Role.RoleName.SELLER).get());

                // 🔥 Đánh dấu seller nhưng đã hết hạn
                expired.setSellerActive(false);
                expired.setSellerExpiryDate(LocalDateTime.now().minusDays(10));
                expired.setSellerPackage(null);

                accountRepo.save(expired);

                System.out.println("⭐ Expired seller created");
            }
            System.out.println("✅ Initialization completed!");
        }


    private void createRoleIfMissing(Integer id, Role.RoleName name, String note) {
        if (!roleRepo.existsById(id)) {
            Role role = new Role();
            role.setRoleId(id);
            role.setRoleName(name);
            role.setNote(note);
            roleRepo.save(role);
            System.out.println("✔ Created role: " + name);
        }
    }
}
