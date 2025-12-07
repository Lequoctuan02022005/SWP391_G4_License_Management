package swp391.fa25.lms.controller.blog;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import swp391.fa25.lms.dto.blog.BlogCategoryDTO;
import swp391.fa25.lms.dto.blog.CreateBlogCategoryDTO;
import swp391.fa25.lms.dto.blog.UpdateBlogCategoryDTO;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.service.BlogCategoryService;

/**
 * Blog Category Controller - Traditional MVC Pattern
 * Quản lý danh mục blog - /manager/blog-categories/**
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/manager/blog-categories")
public class BlogCategoryController {

    private final BlogCategoryService categoryService;

    /**
     * Danh sách quản lý category
     * GET /manager/blog-categories
     */
    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping
    public String listCategories(HttpSession session, Model model, RedirectAttributes ra) {
        Account manager = (Account) session.getAttribute("loggedInAccount");
        if (manager == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("manager", manager);

        return "manager/blog-categories";
    }

    /**
     * Form tạo category mới
     * GET /manager/blog-categories/create
     */
    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/create")
    public String createCategoryForm(HttpSession session, Model model, RedirectAttributes ra) {
        Account manager = (Account) session.getAttribute("loggedInAccount");
        if (manager == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        CreateBlogCategoryDTO dto = new CreateBlogCategoryDTO();
        dto.setStatus("ACTIVE"); // Giá trị mặc định
        dto.setDisplayOrder(0); // Giá trị mặc định
        
        model.addAttribute("category", dto);
        model.addAttribute("manager", manager);
        model.addAttribute("isEdit", false);

        return "manager/category-form";
    }

    /**
     * Xử lý tạo category
     * POST /manager/blog-categories/create
     */
    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping("/create")
    public String createCategory(
            @Valid @ModelAttribute("category") CreateBlogCategoryDTO dto,
            BindingResult result,
            HttpSession session,
            Model model,
            RedirectAttributes ra) {

        Account manager = (Account) session.getAttribute("loggedInAccount");
        if (manager == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        if (result.hasErrors()) {
            model.addAttribute("isEdit", false);
            return "manager/category-form";
        }

        try {
            categoryService.createCategory(dto);
            ra.addFlashAttribute("success", "Tạo danh mục thành công!");
            return "redirect:/manager/blog-categories";
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi: " + e.getMessage());
            model.addAttribute("isEdit", false);
            return "manager/category-form";
        }
    }

    /**
     * Form chỉnh sửa category
     * GET /manager/blog-categories/edit/{id}
     */
    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/edit/{id}")
    public String editCategoryForm(
            @PathVariable Long id,
            HttpSession session,
            Model model,
            RedirectAttributes ra) {

        Account manager = (Account) session.getAttribute("loggedInAccount");
        if (manager == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        try {
            BlogCategoryDTO category = categoryService.getCategoryById(id);
            
            UpdateBlogCategoryDTO updateDTO = new UpdateBlogCategoryDTO();
            updateDTO.setCategoryId(category.getCategoryId());
            updateDTO.setCategoryName(category.getCategoryName());
            updateDTO.setDescription(category.getDescription());
            updateDTO.setSlug(category.getSlug());
            updateDTO.setDisplayOrder(category.getDisplayOrder());
            updateDTO.setStatus(category.getStatus());

            model.addAttribute("category", updateDTO);
            model.addAttribute("isEdit", true);
            model.addAttribute("manager", manager);

            return "manager/category-form";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Không tìm thấy danh mục");
            return "redirect:/manager/blog-categories";
        }
    }

    /**
     * Xử lý cập nhật category
     * POST /manager/blog-categories/edit/{id}
     */
    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping("/edit/{id}")
    public String updateCategory(
            @PathVariable Long id,
            @Valid @ModelAttribute("category") UpdateBlogCategoryDTO dto,
            BindingResult result,
            HttpSession session,
            Model model,
            RedirectAttributes ra) {

        Account manager = (Account) session.getAttribute("loggedInAccount");
        if (manager == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        if (result.hasErrors()) {
            model.addAttribute("isEdit", true);
            return "manager/category-form";
        }

        try {
            dto.setCategoryId(id);
            categoryService.updateCategory(dto);
            ra.addFlashAttribute("success", "Cập nhật danh mục thành công!");
            return "redirect:/manager/blog-categories";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            model.addAttribute("isEdit", true);
            return "manager/category-form";
        }
    }

    /**
     * Xóa category
     * GET /manager/blog-categories/delete/{id}
     */
    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/delete/{id}")
    public String deleteCategory(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes ra) {

        Account manager = (Account) session.getAttribute("loggedInAccount");
        if (manager == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        try {
            categoryService.deleteCategory(id);
            ra.addFlashAttribute("success", "Xóa danh mục thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }

        return "redirect:/manager/blog-categories";
    }

    /**
     * Kích hoạt category
     * GET /manager/blog-categories/activate/{id}
     */
    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/activate/{id}")
    public String activateCategory(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes ra) {

        Account manager = (Account) session.getAttribute("loggedInAccount");
        if (manager == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        try {
            categoryService.activateCategory(id);
            ra.addFlashAttribute("success", "Kích hoạt danh mục thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }

        return "redirect:/manager/blog-categories";
    }

    /**
     * Vô hiệu hóa category
     * GET /manager/blog-categories/deactivate/{id}
     */
    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/deactivate/{id}")
    public String deactivateCategory(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes ra) {

        Account manager = (Account) session.getAttribute("loggedInAccount");
        if (manager == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        try {
            categoryService.deactivateCategory(id);
            ra.addFlashAttribute("success", "Vô hiệu hóa danh mục thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }

        return "redirect:/manager/blog-categories";
    }
}
