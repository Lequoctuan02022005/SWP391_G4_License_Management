package swp391.fa25.lms.controller.blog;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import swp391.fa25.lms.dto.blog.*;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Blog;
import swp391.fa25.lms.repository.AccountRepository;
import swp391.fa25.lms.service.BlogService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller cho Manager quản lý Blog
 * Chỉ MANAGER mới có quyền truy cập
 */
@RestController
@RequestMapping("/api/manager/blogs")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('MANAGER')")
public class BlogController {

    private final BlogService blogService;
    private final AccountRepository accountRepository;

    /**
     * Tạo blog mới
     * POST /api/manager/blogs
     */
    @PostMapping
    public ResponseEntity<?> createBlog(
            @Valid @RequestBody CreateBlogDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Account account = accountRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Account not found"));

            BlogDetailDTO blog = blogService.createBlog(dto, account.getAccountId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Blog created successfully");
            response.put("data", blog);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating blog", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Cập nhật blog
     * PUT /api/manager/blogs/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateBlog(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBlogDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Account account = accountRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Account not found"));

            dto.setBlogId(id);
            BlogDetailDTO blog = blogService.updateBlog(dto, account.getAccountId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Blog updated successfully");
            response.put("data", blog);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating blog", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Xóa blog (soft delete - archive)
     * DELETE /api/manager/blogs/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBlog(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Account account = accountRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Account not found"));

            blogService.deleteBlog(id, account.getAccountId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Blog deleted successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting blog", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Xóa vĩnh viễn blog (hard delete) - ADMIN only
     * DELETE /api/manager/blogs/{id}/permanent
     */
    @DeleteMapping("/{id}/permanent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> permanentlyDeleteBlog(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Account account = accountRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Account not found"));

            blogService.permanentlyDeleteBlog(id, account.getAccountId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Blog permanently deleted");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error permanently deleting blog", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Lấy chi tiết blog theo ID
     * GET /api/manager/blogs/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getBlogById(@PathVariable Long id) {
        try {
            BlogDetailDTO blog = blogService.getBlogById(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", blog);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting blog", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Lấy danh sách blog với pagination
     * GET /api/manager/blogs?page=0&size=10&sort=createdAt,desc
     */
    @GetMapping
    public ResponseEntity<?> getAllBlogs(
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

            Page<BlogListItemDTO> blogs = blogService.getAllBlogs(pageable);

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
            log.error("Error getting blogs", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Lấy blog của author hiện tại
     * GET /api/manager/blogs/my-blogs
     */
    @GetMapping("/my-blogs")
    public ResponseEntity<?> getMyBlogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Account account = accountRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Account not found"));

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<BlogListItemDTO> blogs = blogService.getBlogsByAuthor(account.getAccountId(), pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", blogs.getContent());
            response.put("currentPage", blogs.getNumber());
            response.put("totalPages", blogs.getTotalPages());
            response.put("totalElements", blogs.getTotalElements());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting my blogs", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Advanced search blogs
     * POST /api/manager/blogs/search
     */
    @PostMapping("/search")
    public ResponseEntity<?> searchBlogs(@RequestBody BlogSearchRequestDTO searchRequest) {
        try {
            Page<BlogListItemDTO> blogs = blogService.advancedSearchBlogs(searchRequest);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", blogs.getContent());
            response.put("currentPage", blogs.getNumber());
            response.put("totalPages", blogs.getTotalPages());
            response.put("totalElements", blogs.getTotalElements());

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
     * Publish blog
     * PUT /api/manager/blogs/{id}/publish
     */
    @PutMapping("/{id}/publish")
    public ResponseEntity<?> publishBlog(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Account account = accountRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Account not found"));

            BlogDetailDTO blog = blogService.publishBlog(id, account.getAccountId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Blog published successfully");
            response.put("data", blog);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error publishing blog", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Unpublish blog (chuyển về DRAFT)
     * PUT /api/manager/blogs/{id}/unpublish
     */
    @PutMapping("/{id}/unpublish")
    public ResponseEntity<?> unpublishBlog(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Account account = accountRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Account not found"));

            BlogDetailDTO blog = blogService.unpublishBlog(id, account.getAccountId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Blog unpublished successfully");
            response.put("data", blog);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error unpublishing blog", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Archive blog
     * PUT /api/manager/blogs/{id}/archive
     */
    @PutMapping("/{id}/archive")
    public ResponseEntity<?> archiveBlog(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Account account = accountRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Account not found"));

            BlogDetailDTO blog = blogService.archiveBlog(id, account.getAccountId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Blog archived successfully");
            response.put("data", blog);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error archiving blog", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Lấy thống kê blog
     * GET /api/manager/blogs/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getBlogStats(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            Account account = accountRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Account not found"));

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalBlogs", blogService.countBlogsByAuthor(account.getAccountId()));
            stats.put("publishedBlogs", blogService.countBlogsByStatus(Blog.Status.PUBLISHED));
            stats.put("draftBlogs", blogService.countBlogsByStatus(Blog.Status.DRAFT));
            stats.put("archivedBlogs", blogService.countBlogsByStatus(Blog.Status.ARCHIVED));

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting blog stats", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
