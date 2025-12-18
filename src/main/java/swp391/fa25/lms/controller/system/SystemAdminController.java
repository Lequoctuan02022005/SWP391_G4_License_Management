package swp391.fa25.lms.controller.system;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Role;
import swp391.fa25.lms.repository.RoleRepository;
import swp391.fa25.lms.service.RoleService;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class SystemAdminController {

    private final RoleRepository roleRepository;
    private final RoleService roleService;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model, RedirectAttributes ra) {
        Account admin = (Account) session.getAttribute("loggedInAccount");
        if (admin == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }
        model.addAttribute("account", admin);
        return "admin/dashboard";
    }

    // ROLE MANAGEMENT

    @GetMapping("/roles")
    public String listRoles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "") String keyword,
            HttpSession session,
            Model model,
            RedirectAttributes ra) {

        Account admin = (Account) session.getAttribute("loggedInAccount");
        if (admin == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

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
        model.addAttribute("account", admin); // Sidebar needs this

        return "admin/role-list";
    }

    @GetMapping("/roles/create")
    public String createRoleForm(HttpSession session, Model model, RedirectAttributes ra) {
        Account admin = (Account) session.getAttribute("loggedInAccount");
        if (admin == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }
        model.addAttribute("account", admin);
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
    public String editRoleForm(@PathVariable Integer id, HttpSession session, Model model, RedirectAttributes ra) {
        Account admin = (Account) session.getAttribute("loggedInAccount");
        if (admin == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }
        
        Role role = roleRepository.findById(id).orElse(null);
        if (role == null)
            return "redirect:/admin/roles";

        model.addAttribute("account", admin);
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
