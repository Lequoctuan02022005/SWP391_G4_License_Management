package swp391.fa25.lms.controller.blog;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import swp391.fa25.lms.dto.blog.BlogDetailDTO;
import swp391.fa25.lms.dto.blog.BlogListItemDTO;
import swp391.fa25.lms.service.BlogService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller public cho Blog (Guest/User có thể truy cập)
 * Không cần authentication
 */
@RestController
@RequestMapping("/api/public/blogs")
@RequiredArgsConstructor
@Slf4j
public class BlogPublicController {

    private final BlogService blogService;

    /**
     * Lấy danh sách blog published với pagination
     * GET /api/public/blogs?page=0&size=10
     */
    @GetMapping
    public ResponseEntity<?> getPublishedBlogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        try {
            Sort sort = Sort.by(
                    sortDirection.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC,
                    sortBy
            );
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<BlogListItemDTO> blogs = blogService.getPublishedBlogs(pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", blogs.getContent());
            response.put("currentPage", blogs.getNumber());
            response.put("totalPages", blogs.getTotalPages());
            response.put("totalElements", blogs.getTotalElements());
            response.put("pageSize", blogs.getSize());
            response.put("hasNext", blogs.hasNext());
            response.put("hasPrevious", blogs.hasPrevious());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting published blogs", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Lấy chi tiết blog theo slug (SEO-friendly URL)
     * Tự động tăng view count
     * GET /api/public/blogs/slug/{slug}
     */
    @GetMapping("/slug/{slug}")
    public ResponseEntity<?> getBlogBySlug(@PathVariable String slug) {
        try {
            BlogDetailDTO blog = blogService.getBlogBySlugAndIncrementView(slug);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", blog);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting blog by slug", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Lấy blog theo category
     * GET /api/public/blogs/category/{categoryId}
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<?> getBlogsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<BlogListItemDTO> blogs = blogService.getBlogsByCategory(categoryId, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", blogs.getContent());
            response.put("currentPage", blogs.getNumber());
            response.put("totalPages", blogs.getTotalPages());
            response.put("totalElements", blogs.getTotalElements());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting blogs by category", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Search blog theo keyword
     * GET /api/public/blogs/search?keyword=...
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchBlogs(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<BlogListItemDTO> blogs = blogService.searchBlogs(keyword, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", blogs.getContent());
            response.put("currentPage", blogs.getNumber());
            response.put("totalPages", blogs.getTotalPages());
            response.put("totalElements", blogs.getTotalElements());
            response.put("keyword", keyword);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error searching blogs", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Lấy blog featured (nổi bật)
     * GET /api/public/blogs/featured
     */
    @GetMapping("/featured")
    public ResponseEntity<?> getFeaturedBlogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<BlogListItemDTO> blogs = blogService.getFeaturedBlogs(pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", blogs.getContent());
            response.put("currentPage", blogs.getNumber());
            response.put("totalPages", blogs.getTotalPages());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting featured blogs", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Lấy top blog theo view count
     * GET /api/public/blogs/top-viewed?limit=5
     */
    @GetMapping("/top-viewed")
    public ResponseEntity<?> getTopViewedBlogs(@RequestParam(defaultValue = "5") int limit) {
        try {
            List<BlogListItemDTO> blogs = blogService.getTopViewedBlogs(limit);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", blogs);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting top viewed blogs", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Lấy blog liên quan (cùng category)
     * GET /api/public/blogs/{blogId}/related?limit=5
     */
    @GetMapping("/{blogId}/related")
    public ResponseEntity<?> getRelatedBlogs(
            @PathVariable Long blogId,
            @RequestParam(defaultValue = "5") int limit) {
        try {
            List<BlogListItemDTO> blogs = blogService.getRelatedBlogs(blogId, limit);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", blogs);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting related blogs", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
