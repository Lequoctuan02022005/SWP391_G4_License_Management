package swp391.fa25.lms.controller.system;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Role;
import swp391.fa25.lms.repository.admin.RoleRepository;
import swp391.fa25.lms.service.admin.AccountService;
import swp391.fa25.lms.service.admin.RoleService;

import javax.sql.DataSource;

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
            @Valid @ModelAttribute("account") Account account,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("roles", roleRepository.findAll());
            return "admin/account-edit";
        }

        accountService.update(id, account);
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
}
