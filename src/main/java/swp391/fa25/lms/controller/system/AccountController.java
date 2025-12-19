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
import swp391.fa25.lms.model.Role;
import swp391.fa25.lms.repository.RoleRepository;
import swp391.fa25.lms.service.AccountService;

/**
 * Account Management Controller
 * - Admin: quản lý MOD, MANAGER
 * - Mod: quản lý CUSTOMER, SELLER
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/accounts")
@PreAuthorize("hasAnyRole('ADMIN','MOD')")
public class AccountController {

    private final AccountService accountService;
    private final RoleRepository roleRepository;

    /**
     * Helper method: Lấy danh sách role được phép quản lý theo role hiện tại
     */
    private java.util.List<Role> getAllowedRoles(Role.RoleName currentRole) {
        var allRoles = roleRepository.findAll();
        if (currentRole == Role.RoleName.ADMIN) {
            return allRoles.stream()
                    .filter(r -> r.getRoleName() == Role.RoleName.MOD
                            || r.getRoleName() == Role.RoleName.MANAGER)
                    .toList();
        } else if (currentRole == Role.RoleName.MOD) {
            return allRoles.stream()
                    .filter(r -> r.getRoleName() == Role.RoleName.CUSTOMER
                            || r.getRoleName() == Role.RoleName.SELLER)
                    .toList();
        }
        return java.util.List.of();
    }

    /**
     * Helper method: Kiểm tra quyền quản lý account dựa trên role
     */
    private boolean canManageAccount(Role.RoleName currentRole, Role.RoleName targetRole) {
        return (currentRole == Role.RoleName.ADMIN &&
                (targetRole == Role.RoleName.MOD || targetRole == Role.RoleName.MANAGER))
                || (currentRole == Role.RoleName.MOD &&
                (targetRole == Role.RoleName.CUSTOMER || targetRole == Role.RoleName.SELLER));
    }

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

        Account current = (Account) session.getAttribute("loggedInAccount");
        if (current == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        try {
            // Tạo Pageable với sort theo createdAt desc
            Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
            Pageable pageable = PageRequest.of(page, size, sort);

            // Xác định role hiện tại
            Role.RoleName currentRole = current.getRole() != null ? current.getRole().getRoleName() : null;

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
                // Apply filters theo role hiện tại
                if (currentRole == Role.RoleName.ADMIN) {
                    accountPage = accountService.searchWithFilters(keyword, roleId, accountStatus, pageable);
                } else if (currentRole == Role.RoleName.MOD) {
                    accountPage = accountService.searchWithFiltersForModerator(keyword, roleId, accountStatus, pageable);
                } else {
                    ra.addFlashAttribute("error", "Bạn không có quyền xem danh sách tài khoản.");
                    return "redirect:/home";
                }
            } else {
                // No filter, get all theo role hiện tại
                if (currentRole == Role.RoleName.ADMIN) {
                    accountPage = accountService.getAll(pageable);
                } else if (currentRole == Role.RoleName.MOD) {
                    accountPage = accountService.getAllForModerator(pageable);
                } else {
                    ra.addFlashAttribute("error", "Bạn không có quyền xem danh sách tài khoản.");
                    return "redirect:/home";
                }
            }

            // Load roles cho dropdown filter theo role hiện tại
            java.util.List<Role> roles = getAllowedRoles(currentRole);

            model.addAttribute("accounts", accountPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", accountPage.getTotalPages());
            model.addAttribute("totalAccounts", accountPage.getTotalElements());
            model.addAttribute("pageSize", size);
            model.addAttribute("keyword", keyword);
            model.addAttribute("selectedRole", roleId);
            model.addAttribute("selectedStatus", status);
            model.addAttribute("roles", roles);
            model.addAttribute("account", current); // Sidebar needs this

            return "system/account-list";
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi tải danh sách tài khoản: " + e.getMessage());
            model.addAttribute("accounts", new java.util.ArrayList<>());
            model.addAttribute("account", current);
            return "system/account-list";
        }
    }

    /**
     * Form tạo account mới
     * GET /admin/accounts/create
     */
    @GetMapping("/create")
    public String createForm(HttpSession session, Model model, RedirectAttributes ra) {
        Account current = (Account) session.getAttribute("loggedInAccount");
        if (current == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        Role.RoleName currentRole = current.getRole() != null ? current.getRole().getRoleName() : null;

        // Chỉ ADMIN và MOD được tạo account
        if (currentRole != Role.RoleName.ADMIN && currentRole != Role.RoleName.MOD) {
            ra.addFlashAttribute("error", "Bạn không có quyền tạo tài khoản.");
            return "redirect:/home";
        }

        java.util.List<Role> roles = getAllowedRoles(currentRole);

        model.addAttribute("account", current); // Sidebar needs this
        model.addAttribute("newAccount", new Account());
        model.addAttribute("roles", roles);
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
            RedirectAttributes ra,
            @RequestParam(name = "roleId", required = false) Integer roleId) {

        Account current = (Account) session.getAttribute("loggedInAccount");
        if (current == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }
        Role.RoleName currentRole = current.getRole() != null ? current.getRole().getRoleName() : null;

        if (result.hasErrors()) {
            model.addAttribute("account", current);
            model.addAttribute("roles", getAllowedRoles(currentRole));
            model.addAttribute("isEdit", false);
            return "system/account-form";
        }

        try {
            // Đảm bảo đã chọn role (dựa trên tham số roleId từ form)
            if (roleId == null) {
                throw new RuntimeException("Vui lòng chọn vai trò cho tài khoản.");
            }

            // Lấy role thực tế từ DB để kiểm tra
            Role targetRole = roleRepository.findById(roleId)
                    .orElseThrow(() -> new RuntimeException("Role không tồn tại"));

            if (!canManageAccount(currentRole, targetRole.getRoleName())) {
                throw new RuntimeException("Bạn không có quyền tạo tài khoản với vai trò này.");
            }

            // Gán role đã kiểm tra vào account
            newAccount.setRole(targetRole);

            accountService.create(newAccount);
            ra.addFlashAttribute("success", "Tạo tài khoản thành công!");
            return "redirect:/admin/accounts";
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi: " + e.getMessage());
            model.addAttribute("account", current);
            model.addAttribute("roles", getAllowedRoles(currentRole));
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

        Account current = (Account) session.getAttribute("loggedInAccount");
        if (current == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }
        Role.RoleName currentRole = current.getRole() != null ? current.getRole().getRoleName() : null;

        try {
            Account acc = accountService.getById(id);
            if (acc == null) {
                ra.addFlashAttribute("error", "Không tìm thấy tài khoản");
                return "redirect:/admin/accounts";
            }

            // Kiểm tra quyền chỉnh sửa tài khoản này
            Role.RoleName targetRole = acc.getRole() != null ? acc.getRole().getRoleName() : null;
            if (!canManageAccount(currentRole, targetRole)) {
                ra.addFlashAttribute("error", "Bạn không có quyền chỉnh sửa tài khoản này.");
                return "redirect:/admin/accounts";
            }

            model.addAttribute("account", current);
            model.addAttribute("editAccount", acc);
            model.addAttribute("roles", getAllowedRoles(currentRole));
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

        Account current = (Account) session.getAttribute("loggedInAccount");
        if (current == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }
        Role.RoleName currentRole = current.getRole() != null ? current.getRole().getRoleName() : null;

        try {
            Account targetAccount = accountService.getById(id);
            if (targetAccount == null) {
                ra.addFlashAttribute("error", "Không tìm thấy tài khoản");
                return "redirect:/admin/accounts";
            }

            Role.RoleName targetCurrentRole = targetAccount.getRole() != null ? targetAccount.getRole().getRoleName() : null;

            // Không cho phép chỉnh sửa tài khoản ngoài phạm vi quản lý
            if (!canManageAccount(currentRole, targetCurrentRole)) {
                ra.addFlashAttribute("error", "Bạn không có quyền chỉnh sửa tài khoản này.");
                return "redirect:/admin/accounts";
            }

            // Không cho phép gán role mới ngoài phạm vi quản lý
            Role newRole = roleRepository.findById(roleId)
                    .orElseThrow(() -> new RuntimeException("Role không tồn tại"));

            if (!canManageAccount(currentRole, newRole.getRoleName())) {
                ra.addFlashAttribute("error", "Bạn không có quyền gán vai trò này cho tài khoản.");
                return "redirect:/admin/accounts/edit/" + id;
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

        Account current = (Account) session.getAttribute("loggedInAccount");
        if (current == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        try {
            Account targetAccount = accountService.getById(id);
            if (targetAccount == null) {
                ra.addFlashAttribute("error", "Không tìm thấy tài khoản");
                return "redirect:/admin/accounts";
            }

            Role.RoleName currentRole = current.getRole() != null ? current.getRole().getRoleName() : null;
            Role.RoleName targetRole = targetAccount.getRole() != null ? targetAccount.getRole().getRoleName() : null;

            if (!canManageAccount(currentRole, targetRole)) {
                ra.addFlashAttribute("error", "Bạn không có quyền kích hoạt tài khoản này.");
                return "redirect:/admin/accounts";
            }

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

        Account current = (Account) session.getAttribute("loggedInAccount");
        if (current == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        try {
            Account targetAccount = accountService.getById(id);
            if (targetAccount == null) {
                ra.addFlashAttribute("error", "Không tìm thấy tài khoản");
                return "redirect:/admin/accounts";
            }

            Role.RoleName currentRole = current.getRole() != null ? current.getRole().getRoleName() : null;
            Role.RoleName targetRole = targetAccount.getRole() != null ? targetAccount.getRole().getRoleName() : null;

            if (!canManageAccount(currentRole, targetRole)) {
                ra.addFlashAttribute("error", "Bạn không có quyền vô hiệu hóa tài khoản này.");
                return "redirect:/admin/accounts";
            }

            // Không cho phép deactivate admin account
            if (targetAccount.getRole() != null && 
                targetAccount.getRole().getRoleName() == swp391.fa25.lms.model.Role.RoleName.ADMIN) {
                ra.addFlashAttribute("error", "Không thể vô hiệu hóa tài khoản Admin!");
                return "redirect:/admin/accounts";
            }

            // Không cho phép tự deactivate chính mình
            if (targetAccount.getAccountId().equals(current.getAccountId())) {
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
