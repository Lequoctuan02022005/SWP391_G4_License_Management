package swp391.fa25.lms.controller.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.repository.RoleRepository;
import swp391.fa25.lms.service.AccountService;

@Controller
@RequestMapping("/admin/accounts")
public class SystemAdminController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private RoleRepository roleRepository;


    @GetMapping
    public String listAccounts(Model model) {
        model.addAttribute("accounts", accountService.getAll());
        return "admin/account-list";  // ➜ view Thymeleaf
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("account", new Account());
        model.addAttribute("roles", roleRepository.findAll());
        return "admin/account-create";
    }

    @PostMapping("/create")
    public String createAccount(@ModelAttribute Account account) {

        accountService.create(account);
        return "redirect:/admin/accounts";
    }


    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        Account acc = accountService.getById(id);
        if (acc == null) {
            return "redirect:/admin/accounts";
        }

        model.addAttribute("account", acc);
        model.addAttribute("roles", roleRepository.findAll());

        return "admin/account-edit";
    }

    @PostMapping("/edit/{id}")
    public String editAccount(@PathVariable Long id, @ModelAttribute Account account) {

        accountService.update(id, account);

        return "redirect:/admin/accounts";
    }

    @GetMapping("/delete/{id}")
    public String deleteAccount(@PathVariable Long id) {
        accountService.delete(id);
        return "redirect:/admin/accounts";
    }
}
