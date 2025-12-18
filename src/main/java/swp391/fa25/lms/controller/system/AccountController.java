package swp391.fa25.lms.controller.system;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.repository.RoleRepository;
import swp391.fa25.lms.service.AccountService;

/**
 * Account Management Controller - Admin Only
 * Quản lý tài khoản: /admin/accounts/**
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/accounts")
@PreAuthorize("hasRole('ADMIN')")
public class AccountController {

    private final AccountService accountService;
    private final RoleRepository roleRepository;

    /**
     * Danh sách account với search, filter và phân trang
     * GET /admin/accounts
     */
    @GetMapping
    public String listAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer roleId,
            @RequestParam(required = false) String status,
            HttpSession session,
            Model model,
            RedirectAttributes ra) {

        Account admin = (Account) session.getAttribute("loggedInAccount");
        if (admin == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        try {
            // Tạo Pageable với sort theo createdAt desc
            Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
            Pageable pageable = PageRequest.of(page, size, sort);

            // Convert status string to enum
            Account.AccountStatus accountStatus = null;
            if (status != null && !status.trim().isEmpty()) {
                try {
                    accountStatus = Account.AccountStatus.valueOf(status.toUpperCase());
                } catch (IllegalArgumentException e) {
                    // Invalid status, ignore
                }
            }

            // Lấy tất cả accounts với filter
            Page<Account> accountPage;
            
            if ((keyword != null && !keyword.trim().isEmpty()) || roleId != null || accountStatus != null) {
                // Apply filters
                accountPage = accountService.searchWithFilters(keyword, roleId, accountStatus, pageable);
            } else {
                // No filter, get all
                accountPage = accountService.getAll(pageable);
            }

            // Load roles for filter dropdown
            var roles = roleRepository.findAll();

            model.addAttribute("accounts", accountPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", accountPage.getTotalPages());
            model.addAttribute("totalAccounts", accountPage.getTotalElements());
            model.addAttribute("pageSize", size);
            model.addAttribute("keyword", keyword);
            model.addAttribute("selectedRole", roleId);
            model.addAttribute("selectedStatus", status);
            model.addAttribute("roles", roles);
            model.addAttribute("account", admin); // Sidebar needs this

            return "system/account-list";
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi tải danh sách tài khoản: " + e.getMessage());
            model.addAttribute("accounts", new java.util.ArrayList<>());
            model.addAttribute("account", admin);
            return "system/account-list";
        }
    }

    /**
     * Form tạo account mới
     * GET /admin/accounts/create
     */
    @GetMapping("/create")
    public String createForm(HttpSession session, Model model, RedirectAttributes ra) {
        Account admin = (Account) session.getAttribute("loggedInAccount");
        if (admin == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        model.addAttribute("account", admin); // Sidebar needs this
        model.addAttribute("newAccount", new Account());
        model.addAttribute("roles", roleRepository.findAll());
        model.addAttribute("isEdit", false);

        return "system/account-form";
    }

    /**
     * Xử lý tạo account mới
     * POST /admin/accounts/create
     */
    @PostMapping("/create")
    public String createAccount(
            @Valid @ModelAttribute("newAccount") Account newAccount,
            BindingResult result,
            HttpSession session,
            Model model,
            RedirectAttributes ra) {

        Account admin = (Account) session.getAttribute("loggedInAccount");
        if (admin == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        if (result.hasErrors()) {
            model.addAttribute("account", admin);
            model.addAttribute("roles", roleRepository.findAll());
            model.addAttribute("isEdit", false);
            return "system/account-form";
        }

        try {
            accountService.create(newAccount);
            ra.addFlashAttribute("success", "Tạo tài khoản thành công!");
            return "redirect:/admin/accounts";
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi: " + e.getMessage());
            model.addAttribute("account", admin);
            model.addAttribute("roles", roleRepository.findAll());
            model.addAttribute("isEdit", false);
            return "system/account-form";
        }
    }

    /**
     * Form chỉnh sửa account
     * GET /admin/accounts/edit/{id}
     */
    @GetMapping("/edit/{id}")
    public String editForm(
            @PathVariable Long id,
            HttpSession session,
            Model model,
            RedirectAttributes ra) {

        Account admin = (Account) session.getAttribute("loggedInAccount");
        if (admin == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        try {
            Account acc = accountService.getById(id);
            if (acc == null) {
                ra.addFlashAttribute("error", "Không tìm thấy tài khoản");
                return "redirect:/admin/accounts";
            }

            model.addAttribute("account", admin); // Sidebar needs this
            model.addAttribute("editAccount", acc);
            model.addAttribute("roles", roleRepository.findAll());
            model.addAttribute("isEdit", true);

            return "system/account-form";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Không tìm thấy tài khoản: " + e.getMessage());
            return "redirect:/admin/accounts";
        }
    }

    /**
     * Xử lý cập nhật account
     * POST /admin/accounts/edit/{id}
     */
    @PostMapping("/edit/{id}")
    public String updateAccount(
            @PathVariable Long id,
            @RequestParam String fullName,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String address,
            @RequestParam Account.AccountStatus status,
            @RequestParam Integer roleId,
            HttpSession session,
            RedirectAttributes ra) {

        Account admin = (Account) session.getAttribute("loggedInAccount");
        if (admin == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        try {
            Account targetAccount = accountService.getById(id);
            if (targetAccount == null) {
                ra.addFlashAttribute("error", "Không tìm thấy tài khoản");
                return "redirect:/admin/accounts";
            }

            // Không cho phép deactivate admin account qua update
            if (targetAccount.getRole() != null && 
                targetAccount.getRole().getRoleName() == swp391.fa25.lms.model.Role.RoleName.ADMIN &&
                status == Account.AccountStatus.DEACTIVATED) {
                ra.addFlashAttribute("error", "Không thể vô hiệu hóa tài khoản Admin!");
                return "redirect:/admin/accounts/edit/" + id;
            }

            accountService.updateWithRoleFields(id, fullName, phone, address, status, roleId);
            ra.addFlashAttribute("success", "Cập nhật tài khoản thành công!");
            return "redirect:/admin/accounts";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/admin/accounts/edit/" + id;
        }
    }

    /**
     * Kích hoạt account
     * POST /admin/accounts/activate/{id}
     */
    @PostMapping("/activate/{id}")
    public String activateAccount(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes ra) {

        Account admin = (Account) session.getAttribute("loggedInAccount");
        if (admin == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        try {
            accountService.activateAccount(id);
            ra.addFlashAttribute("success", "Kích hoạt tài khoản thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }

        return "redirect:/admin/accounts";
    }

    /**
     * Vô hiệu hóa account
     * POST /admin/accounts/deactivate/{id}
     */
    @PostMapping("/deactivate/{id}")
    public String deactivateAccount(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes ra) {

        Account admin = (Account) session.getAttribute("loggedInAccount");
        if (admin == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        try {
            Account targetAccount = accountService.getById(id);
            if (targetAccount == null) {
                ra.addFlashAttribute("error", "Không tìm thấy tài khoản");
                return "redirect:/admin/accounts";
            }

            // Không cho phép deactivate admin account
            if (targetAccount.getRole() != null && 
                targetAccount.getRole().getRoleName() == swp391.fa25.lms.model.Role.RoleName.ADMIN) {
                ra.addFlashAttribute("error", "Không thể vô hiệu hóa tài khoản Admin!");
                return "redirect:/admin/accounts";
            }

            // Không cho phép tự deactivate chính mình
            if (targetAccount.getAccountId().equals(admin.getAccountId())) {
                ra.addFlashAttribute("error", "Không thể vô hiệu hóa chính tài khoản của bạn!");
                return "redirect:/admin/accounts";
            }

            accountService.deactivateAccount(id);
            ra.addFlashAttribute("success", "Vô hiệu hóa tài khoản thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }

        return "redirect:/admin/accounts";
    }
}
