package swp391.fa25.lms.controller.blog;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp391.fa25.lms.dto.blog.BlogCategoryDTO;
import swp391.fa25.lms.dto.blog.CreateBlogCategoryDTO;
import swp391.fa25.lms.dto.blog.UpdateBlogCategoryDTO;
import swp391.fa25.lms.model.BlogCategory;
import swp391.fa25.lms.service.BlogCategoryService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller cho Manager quản lý BlogCategory
 * Chỉ MANAGER mới có quyền truy cập
 */
@RestController
@RequestMapping("/api/manager/blog-categories")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('MANAGER')")
public class BlogCategoryController {

    private final BlogCategoryService categoryService;

    /**
     * Tạo category mới
     * POST /api/manager/blog-categories
     */
    @PostMapping
    public ResponseEntity<?> createCategory(@Valid @RequestBody CreateBlogCategoryDTO dto) {
        try {
            BlogCategoryDTO category = categoryService.createCategory(dto);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Category created successfully");
            response.put("data", category);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating category", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Cập nhật category
     * PUT /api/manager/blog-categories/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBlogCategoryDTO dto) {
        try {
            dto.setCategoryId(id);
            BlogCategoryDTO category = categoryService.updateCategory(dto);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Category updated successfully");
            response.put("data", category);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating category", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Xóa category (chỉ nếu không có blog)
     * DELETE /api/manager/blog-categories/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        try {
            categoryService.deleteCategory(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Category deleted successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting category", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Lấy chi tiết category theo ID
     * GET /api/manager/blog-categories/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getCategoryById(@PathVariable Long id) {
        try {
            BlogCategoryDTO category = categoryService.getCategoryById(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", category);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting category", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Lấy tất cả categories (bao gồm INACTIVE)
     * GET /api/manager/blog-categories
     */
    @GetMapping
    public ResponseEntity<?> getAllCategories() {
        try {
            List<BlogCategoryDTO> categories = categoryService.getAllCategories();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", categories);
            response.put("total", categories.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting categories", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Lấy categories kèm số lượng blog
     * GET /api/manager/blog-categories/with-blog-count
     */
    @GetMapping("/with-blog-count")
    public ResponseEntity<?> getCategoriesWithBlogCount() {
        try {
            List<BlogCategoryDTO> categories = categoryService.getCategoriesWithBlogCount();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", categories);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting categories with blog count", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Swap displayOrder giữa 2 categories
     * PUT /api/manager/blog-categories/reorder
     */
    @PutMapping("/reorder")
    public ResponseEntity<?> reorderCategories(
            @RequestParam Long categoryId1,
            @RequestParam Long categoryId2) {
        try {
            categoryService.reorderCategories(categoryId1, categoryId2);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Categories reordered successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error reordering categories", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Update displayOrder của category
     * PUT /api/manager/blog-categories/{id}/display-order
     */
    @PutMapping("/{id}/display-order")
    public ResponseEntity<?> updateDisplayOrder(
            @PathVariable Long id,
            @RequestParam Integer newDisplayOrder) {
        try {
            categoryService.updateDisplayOrder(id, newDisplayOrder);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Display order updated successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating display order", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Activate category
     * PUT /api/manager/blog-categories/{id}/activate
     */
    @PutMapping("/{id}/activate")
    public ResponseEntity<?> activateCategory(@PathVariable Long id) {
        try {
            BlogCategoryDTO category = categoryService.activateCategory(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Category activated successfully");
            response.put("data", category);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error activating category", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Deactivate category
     * PUT /api/manager/blog-categories/{id}/deactivate
     */
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivateCategory(@PathVariable Long id) {
        try {
            BlogCategoryDTO category = categoryService.deactivateCategory(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Category deactivated successfully");
            response.put("data", category);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deactivating category", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Lấy thống kê categories
     * GET /api/manager/blog-categories/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getCategoryStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalCategories", categoryService.countByStatus(BlogCategory.Status.ACTIVE) +
                    categoryService.countByStatus(BlogCategory.Status.INACTIVE));
            stats.put("activeCategories", categoryService.countByStatus(BlogCategory.Status.ACTIVE));
            stats.put("inactiveCategories", categoryService.countByStatus(BlogCategory.Status.INACTIVE));

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting category stats", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Kiểm tra category name đã tồn tại chưa
     * GET /api/manager/blog-categories/check-name?name=...
     */
    @GetMapping("/check-name")
    public ResponseEntity<?> checkCategoryName(@RequestParam String name) {
        try {
            boolean exists = categoryService.isCategoryNameExists(name);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("exists", exists);
            response.put("message", exists ? "Category name already exists" : "Category name is available");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error checking category name", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
