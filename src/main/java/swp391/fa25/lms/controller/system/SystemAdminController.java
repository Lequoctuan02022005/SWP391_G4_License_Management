package swp391.fa25.lms.controller.system;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Role;
import swp391.fa25.lms.model.SellerSubscription;
import swp391.fa25.lms.repository.RoleRepository;
import swp391.fa25.lms.service.RoleService;
import swp391.fa25.lms.service.AccountService;
import swp391.fa25.lms.service.SellerSubscriptionService;

import javax.sql.DataSource;
import java.time.LocalDate;

@Controller
@RequestMapping("/admin")
public class SystemAdminController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RoleService roleService;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private SellerSubscriptionService sellerSubscriptionService;


    @GetMapping("/dashboard")
    public String dashboard() {
        return "admin/dashboard";
    }


    // ACCOUNT MANAGEMENT
    @GetMapping("/accounts")
    public String listAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "") String keyword,
            Model model) {

        Pageable pageable = PageRequest.of(page, 4);
        Page<Account> accountPage;

        if (!keyword.isBlank()) {
            accountPage = accountService.search(keyword.trim(), pageable);
        } else {
            accountPage = accountService.getAll(pageable);
        }

        model.addAttribute("accounts", accountPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", accountPage.getTotalPages());
        model.addAttribute("keyword", keyword);

        return "admin/account-list";
    }

    @GetMapping("/accounts/create")
    public String createForm(Model model) {
        model.addAttribute("account", new Account());
        model.addAttribute("roles", roleRepository.findAll());
        return "admin/account-create";
    }

    @PostMapping("/accounts/create")
    public String createAccount(
            @Valid @ModelAttribute("account") Account account,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("roles", roleRepository.findAll());
            return "admin/account-create"; // quay lại form và hiển thị lỗi
        }

        accountService.create(account);
        return "redirect:/admin/accounts";
    }

    @GetMapping("/accounts/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        Account acc = accountService.getById(id);
        if (acc == null) {
            return "redirect:/admin/accounts";
        }

        model.addAttribute("account", acc);
        model.addAttribute("roles", roleRepository.findAll());

        return "admin/account-edit";
    }

    @PostMapping("/accounts/edit/{id}")
    public String editAccount(
            @PathVariable Long id,
            @RequestParam String fullName,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String address,
            @RequestParam Account.AccountStatus status,
            @RequestParam Integer roleId
    ) {
        accountService.updateWithRoleFields(
                id, fullName, phone, address, status, roleId
        );
        return "redirect:/admin/accounts";
    }



    @GetMapping("/accounts/delete/{id}")
    public String deleteAccount(@PathVariable Long id) {
        accountService.delete(id);
        return "redirect:/admin/accounts";
    }

    // ROLE MANAGEMENT

    @GetMapping("/roles")
    public String listRoles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "") String keyword,
            Model model) {

        Pageable pageable = PageRequest.of(page, 5); // 5 item mỗi trang
        Page<Role> rolePage;

        if (!keyword.isBlank()) {
            rolePage = roleService.search(keyword.trim(), pageable);
        } else {
            rolePage = roleService.getAll(pageable);
        }

        model.addAttribute("roles", rolePage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", rolePage.getTotalPages());
        model.addAttribute("keyword", keyword);

        return "admin/role-list";
    }

    @GetMapping("/roles/create")
    public String createRoleForm(Model model) {
        model.addAttribute("role", new Role());
        return "admin/role-create";
    }

    @PostMapping("/roles/create")
    public String createRole(@ModelAttribute Role role) {

        // Lấy ID lớn nhất hiện tại rồi +1
        Integer newId = roleRepository.findTopByOrderByRoleIdDesc()
                .map(r -> r.getRoleId() + 1)
                .orElse(1);

        role.setRoleId(newId); // tự set ID vì model không tự sinh

        roleRepository.save(role);
        return "redirect:/admin/roles";
    }


    @GetMapping("/roles/edit/{id}")
    public String editRoleForm(@PathVariable Integer id, Model model) {
        Role role = roleRepository.findById(id).orElse(null);
        if (role == null)
            return "redirect:/admin/roles";

        model.addAttribute("role", role);
        return "admin/role-edit";
    }

    @PostMapping("/roles/edit/{id}")
    public String updateRole(@PathVariable Integer id, @ModelAttribute Role newRole) {
        Role r = roleRepository.findById(id).orElse(null);
        if (r != null) {
            r.setNote(newRole.getNote());
            r.setRoleName(newRole.getRoleName());
            roleRepository.save(r);
        }
        return "redirect:/admin/roles";
    }

    @GetMapping("/roles/delete/{id}")
    public String deleteRole(@PathVariable Integer id) {
        roleRepository.deleteById(id);
        return "redirect:/admin/roles";
    }
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/financial-report")
    public String financialReport(
            @RequestParam(required = false) String seller,
            @RequestParam(required = false) Long packageId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {

        Pageable pageable = PageRequest.of(page, 10);

        Page<SellerSubscription> result =
                sellerSubscriptionService.filter(
                        seller, packageId, status, fromDate, toDate, pageable
                );
        Long totalRevenue =
                sellerSubscriptionService.sumRevenue(
                        seller, packageId, status, fromDate, toDate
                );
        model.addAttribute("page", result);
        model.addAttribute("totalRevenue", totalRevenue);

        return "system/financial-report";
    }

}
