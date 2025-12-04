package swp391.fa25.lms.controller.blog;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import swp391.fa25.lms.dto.blog.BlogCategoryDTO;
import swp391.fa25.lms.service.BlogCategoryService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller public cho BlogCategory (Guest/User có thể truy cập)
 * Không cần authentication
 */
@RestController
@RequestMapping("/api/public/blog-categories")
@RequiredArgsConstructor
@Slf4j
public class BlogCategoryPublicController {

    private final BlogCategoryService categoryService;

    /**
     * Lấy tất cả categories ACTIVE (sắp xếp theo displayOrder)
     * GET /api/public/blog-categories
     */
    @GetMapping
    public ResponseEntity<?> getActiveCategories() {
        try {
            List<BlogCategoryDTO> categories = categoryService.getActiveCategories();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", categories);
            response.put("total", categories.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting active categories", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Lấy categories kèm số lượng blog (chỉ ACTIVE)
     * GET /api/public/blog-categories/with-blog-count
     */
    @GetMapping("/with-blog-count")
    public ResponseEntity<?> getCategoriesWithBlogCount() {
        try {
            List<BlogCategoryDTO> categories = categoryService.getCategoriesWithBlogCount();

            // Filter only ACTIVE categories
            List<BlogCategoryDTO> activeCategories = categories.stream()
                    .filter(cat -> "ACTIVE".equals(cat.getStatus()))
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", activeCategories);
            response.put("total", activeCategories.size());

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
     * Lấy category theo slug
     * GET /api/public/blog-categories/slug/{slug}
     */
    @GetMapping("/slug/{slug}")
    public ResponseEntity<?> getCategoryBySlug(@PathVariable String slug) {
        try {
            BlogCategoryDTO category = categoryService.getCategoryBySlug(slug);

            // Check if category is ACTIVE
            if (!"ACTIVE".equals(category.getStatus())) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Category is not active");
                return ResponseEntity.badRequest().body(response);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", category);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting category by slug", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Lấy category theo ID
     * GET /api/public/blog-categories/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getCategoryById(@PathVariable Long id) {
        try {
            BlogCategoryDTO category = categoryService.getCategoryById(id);

            // Check if category is ACTIVE
            if (!"ACTIVE".equals(category.getStatus())) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Category is not active");
                return ResponseEntity.badRequest().body(response);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", category);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting category by ID", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
