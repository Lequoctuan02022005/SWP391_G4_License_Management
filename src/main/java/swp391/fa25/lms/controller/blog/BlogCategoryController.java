package swp391.fa25.lms.controller.blog;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import swp391.fa25.lms.dto.blog.BlogCategoryDTO;
import swp391.fa25.lms.dto.blog.CreateBlogCategoryDTO;
import swp391.fa25.lms.dto.blog.UpdateBlogCategoryDTO;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.service.BlogCategoryService;

import java.util.List;

/**
 * Blog Category Controller - Manager Only
 * Quản lý danh mục blog: /manager/blog-categories/**
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/manager/blog-categories")
@Slf4j
public class BlogCategoryController {

    private final BlogCategoryService categoryService;

    /**
     * Danh sách category với search, sort và phân trang
     * GET /manager/blog-categories
     */
    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping
    public String listCategories(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "displayOrder") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session, 
            Model model, 
            RedirectAttributes ra) {
        
        Account manager = (Account) session.getAttribute("loggedInAccount");
        if (manager == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        try {
            // Tạo Pageable với sort
            Sort sort = Sort.by(Sort.Direction.ASC, sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            
            // Lấy tất cả categories với filter
            List<BlogCategoryDTO> allCategories = categoryService.searchCategories(keyword, status, sortBy);
            
            // Manual pagination
            int start = page * size;
            int end = Math.min(start + size, allCategories.size());
            List<BlogCategoryDTO> pageContent = (start < allCategories.size()) 
                    ? allCategories.subList(start, end) 
                    : new java.util.ArrayList<>();
            
            Page<BlogCategoryDTO> categoriesPage = new org.springframework.data.domain.PageImpl<>(
                    pageContent, pageable, allCategories.size());
            
            model.addAttribute("categories", categoriesPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", categoriesPage.getTotalPages());
            model.addAttribute("totalCategories", categoriesPage.getTotalElements());
            model.addAttribute("pageSize", size);
            model.addAttribute("keyword", keyword);
            model.addAttribute("status", status);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("manager", manager);
            model.addAttribute("account", manager); // Sidebar needs this
            
            return "blog/manager/blog-categories";
        } catch (Exception e) {
            log.error("Error loading categories with keyword={}, status={}, sortBy={}", keyword, status, sortBy, e);
            model.addAttribute("error", "Lỗi tải danh sách danh mục: " + e.getMessage());
            model.addAttribute("categories", new java.util.ArrayList<>());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("totalCategories", 0);
            model.addAttribute("manager", manager);
            model.addAttribute("account", manager); // Sidebar needs this
            return "blog/manager/blog-categories";
        }
    }

    /**
     * Form tạo category mới
     * GET /manager/blog-categories/create
     */
    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/create")
    public String createForm(HttpSession session, Model model, RedirectAttributes ra) {
        Account manager = (Account) session.getAttribute("loggedInAccount");
        if (manager == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        CreateBlogCategoryDTO dto = new CreateBlogCategoryDTO();
        dto.setDisplayOrder(0);

        model.addAttribute("category", dto);
        model.addAttribute("isEdit", false);
        model.addAttribute("manager", manager);
        model.addAttribute("account", manager); // Sidebar needs this

        return "blog/manager/category-form";
    }

    /**
     * Xử lý tạo category
     * POST /manager/blog-categories/create
     */
    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping("/create")
    public String create(
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
            model.addAttribute("manager", manager);
            model.addAttribute("account", manager); // Sidebar needs this
            return "blog/manager/category-form";
        }

        try {
            categoryService.createCategory(dto);
            ra.addFlashAttribute("success", "Tạo danh mục thành công!");
            return "redirect:/manager/blog-categories";
        } catch (RuntimeException e) {
            log.error("Error creating category", e);
            model.addAttribute("error", "Lỗi: " + e.getMessage());
            model.addAttribute("manager", manager);
            model.addAttribute("account", manager); // Sidebar needs this
            return "blog/manager/category-form";
        }
    }

    /**
     * Form sửa category
     * GET /manager/blog-categories/edit/{id}
     */
    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/edit/{id}")
    public String editForm(
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
            model.addAttribute("account", manager); // Sidebar needs this

            return "blog/manager/category-form";
        } catch (RuntimeException e) {
            log.error("Error loading category for edit: {}", id, e);
            ra.addFlashAttribute("error", "Không tìm thấy danh mục");
            return "redirect:/manager/blog-categories";
        }
    }

    /**
     * Xử lý update category
     * POST /manager/blog-categories/edit/{id}
     */
    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping("/edit/{id}")
    public String update(
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
            model.addAttribute("manager", manager);
            model.addAttribute("account", manager); // Sidebar needs this
            return "blog/manager/category-form";
        }

        try {
            // Security: Đảm bảo ID từ path khớp với DTO
            if (dto.getCategoryId() == null || !dto.getCategoryId().equals(id)) {
                dto.setCategoryId(id);
            }

            categoryService.updateCategory(dto);
            ra.addFlashAttribute("success", "Cập nhật danh mục thành công!");
            return "redirect:/manager/blog-categories";
        } catch (RuntimeException e) {
            log.error("Error updating category: {}", id, e);
            dto.setCategoryId(id);
            model.addAttribute("error", "Lỗi: " + e.getMessage());
            model.addAttribute("isEdit", true);
            model.addAttribute("manager", manager);
            model.addAttribute("account", manager); // Sidebar needs this
            return "blog/manager/category-form";
        }
    }

    /**
     * Xóa category
     * GET /manager/blog-categories/delete/{id}
     */
    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/delete/{id}")
    public String delete(
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
        } catch (RuntimeException e) {
            log.error("Error deleting category: {}", id, e);
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
    public String activate(
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
        } catch (RuntimeException e) {
            log.error("Error activating category: {}", id, e);
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
    public String deactivate(
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
        } catch (RuntimeException e) {
            log.error("Error deactivating category: {}", id, e);
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }

        return "redirect:/manager/blog-categories";
    }
}
