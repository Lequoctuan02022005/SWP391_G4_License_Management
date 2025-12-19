package swp391.fa25.lms.controller.blog;

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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import swp391.fa25.lms.dto.blog.*;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Blog;
import swp391.fa25.lms.service.BlogCategoryService;
import swp391.fa25.lms.service.BlogService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

/**
 * Public pages: /blog/**
 * Moderator pages: /moderator/blogs/**
 */
@Controller
@RequiredArgsConstructor
public class BlogController {

    private final BlogService blogService;
    private final BlogCategoryService categoryService;

    // ========================================
    // PUBLIC PAGES - /blog/**
    // ========================================

    /**
     * Trang danh sách blog công khai
     * GET /blog
     */
    @GetMapping("/blog")
    public String blogList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Page<BlogListItemDTO> blogs;
        Long categoryId = null;
        if (category != null && !category.trim().isEmpty()) {
            BlogCategoryDTO categoryDTO = categoryService.getCategoryBySlug(category);
            categoryId = categoryDTO.getCategoryId();
            blogs = blogService.getBlogsByCategory(categoryId, pageable);
        } else if (keyword != null && !keyword.trim().isEmpty()) {
            blogs = blogService.searchBlogs(keyword, pageable);
        } else {
            blogs = blogService.getPublishedBlogs(pageable);
        }

        model.addAttribute("blogs", blogs.getContent());
        model.addAttribute("categories", categoryService.getActiveCategories());
        model.addAttribute("popularBlogs", blogService.getTopViewedBlogs(5));
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", blogs.getTotalPages());
        model.addAttribute("totalBlogs", blogs.getTotalElements());
        model.addAttribute("selectedCategorySlug", category);
        model.addAttribute("keyword", keyword);

        return "blog/blog-list";
    }

    /**
     * Trang chi tiết blog
     * GET /blog/{slug}
     */
    @GetMapping("/blog/{slug}")
    public String blogDetail(@PathVariable String slug, Model model, RedirectAttributes ra) {
        try {
            BlogDetailDTO blog = blogService.getBlogBySlugAndIncrementView(slug);
            
            // Check if blog is published (compare enum)
            if (blog.getStatus() != Blog.Status.PUBLISHED) {
                ra.addFlashAttribute("error", "Blog không tồn tại hoặc chưa được xuất bản");
                return "redirect:/blog";
            }

            model.addAttribute("blog", blog);
            // Related blogs already loaded in BlogDetailDTO.relatedBlogs by service
            model.addAttribute("relatedBlogs", blog.getRelatedBlogs());
            model.addAttribute("popularBlogs", blogService.getTopViewedBlogs(5));
            model.addAttribute("categories", categoryService.getActiveCategories());

            return "blog/blog-detail";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Không tìm thấy blog");
            return "redirect:/blog";
        }
    }

    // ========================================
    // MODERATOR PAGES - /moderator/blogs/**
    // ========================================

    /**
     * Trang quản lý blog (danh sách) - MOD
     * GET /moderator/blogs
     */
    @PreAuthorize("hasRole('MOD')")
    @GetMapping("/moderator/blogs")
    public String blogList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            HttpSession session,
            Model model,
            RedirectAttributes ra) {

        Account moderator = (Account) session.getAttribute("loggedInAccount");
        if (moderator == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Page<BlogListItemDTO> blogs = blogService.getBlogsByAuthor(moderator.getAccountId(), pageable);
        
        // Check if any filter is applied
        boolean hasFilter = (categoryId != null) || (keyword != null && !keyword.trim().isEmpty()) || (status != null && !status.trim().isEmpty());
        
        if (hasFilter) {
            // If filtering, get ALL blogs from author first to calculate correct total
            List<BlogListItemDTO> allBlogs = blogService.getBlogsByAuthor(moderator.getAccountId(), 
                    PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();
            
            // Apply filters
            if (categoryId != null) {
                allBlogs = allBlogs.stream()
                        .filter(blog -> blog.getCategoryId() != null && blog.getCategoryId().equals(categoryId))
                        .collect(java.util.stream.Collectors.toList());
            }
            
            if (keyword != null && !keyword.trim().isEmpty()) {
                String searchLower = keyword.toLowerCase();
                allBlogs = allBlogs.stream()
                        .filter(blog -> (blog.getTitle() != null && blog.getTitle().toLowerCase().contains(searchLower)) ||
                                       (blog.getShortSummary() != null && blog.getShortSummary().toLowerCase().contains(searchLower)))
                        .collect(java.util.stream.Collectors.toList());
            }
            
            if (status != null && !status.trim().isEmpty()) {
                String statusUpper = status.toUpperCase();
                allBlogs = allBlogs.stream()
                        .filter(blog -> blog.getStatus() != null && blog.getStatus().equals(statusUpper))
                        .collect(java.util.stream.Collectors.toList());
            }
            
            // Manual pagination on filtered list
            int start = page * size;
            int end = Math.min(start + size, allBlogs.size());
            List<BlogListItemDTO> pageContent = (start < allBlogs.size()) ? allBlogs.subList(start, end) : new java.util.ArrayList<>();
            
            blogs = new org.springframework.data.domain.PageImpl<>(pageContent, pageable, allBlogs.size());
        }
        // else: use original Page from service (already paginated correctly)

        model.addAttribute("blogs", blogs.getContent());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedCategory", categoryId);
        model.addAttribute("keyword", keyword);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", blogs.getTotalPages());
        model.addAttribute("totalBlogs", blogs.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("moderator", moderator);
        model.addAttribute("account", moderator); // Sidebar needs this

        return "blog/manager/blog-list";
    }

    /**
     * Form tạo blog mới - MOD
     * GET /moderator/blogs/create
     */
    @PreAuthorize("hasRole('MOD')")
    @GetMapping("/moderator/blogs/create")
    public String createBlogForm(HttpSession session, Model model, RedirectAttributes ra) {
        Account moderator = (Account) session.getAttribute("loggedInAccount");
        if (moderator == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        CreateBlogDTO dto = new CreateBlogDTO();
        dto.setStatus("DRAFT");
        
        model.addAttribute("blog", dto);
        model.addAttribute("categories", categoryService.getActiveCategories());
        model.addAttribute("moderator", moderator);
        model.addAttribute("account", moderator); // Sidebar needs this

        return "blog/manager/blog-form";
    }

    /**
     * Xử lý tạo blog mới - MOD
     * POST /moderator/blogs/create
     */
    @PreAuthorize("hasRole('MOD')")
    @PostMapping("/moderator/blogs/create")
    public String createBlog(
            @Valid @ModelAttribute("blog") CreateBlogDTO dto,
            BindingResult result,
            @RequestParam(required = false) MultipartFile thumbnailFile,
            @RequestParam(required = false) MultipartFile bannerFile,
            HttpSession session,
            Model model,
            RedirectAttributes ra) {

        Account moderator = (Account) session.getAttribute("loggedInAccount");
        if (moderator == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.getActiveCategories());
            model.addAttribute("moderator", moderator);
            model.addAttribute("account", moderator); // Sidebar needs this
            return "blog/manager/blog-form";
        }

        try {
            // Handle thumbnail upload
            if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
                String thumbnailPath = saveUploadedFile(thumbnailFile, "images");
                dto.setThumbnailImage(thumbnailPath);
            }
            
            // Handle banner upload
            if (bannerFile != null && !bannerFile.isEmpty()) {
                String bannerPath = saveUploadedFile(bannerFile, "images");
                dto.setBannerImage(bannerPath);
            }
            
            blogService.createBlog(dto, moderator.getAccountId());
            ra.addFlashAttribute("success", "Tạo blog thành công!");
            return "redirect:/moderator/blogs";
        } catch (Exception e) {
            // Hiển thị thông báo lỗi thân thiện (ví dụ ảnh > 5MB, sai định dạng, ...)
            model.addAttribute("error", e.getMessage());
            model.addAttribute("categories", categoryService.getActiveCategories());
            model.addAttribute("moderator", moderator);
            model.addAttribute("account", moderator); // Sidebar needs this
            return "blog/manager/blog-form";
        }
    }

    /**
     * Form chỉnh sửa blog - MOD
     * GET /moderator/blogs/edit/{id}
     */
    @PreAuthorize("hasRole('MOD')")
    @GetMapping("/moderator/blogs/edit/{id}")
    public String editBlogForm(
            @PathVariable Long id,
            HttpSession session,
            Model model,
            RedirectAttributes ra) {

        Account moderator = (Account) session.getAttribute("loggedInAccount");
        if (moderator == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        try {
            BlogDetailDTO blog = blogService.getBlogById(id);
            
            UpdateBlogDTO updateDTO = new UpdateBlogDTO();
            updateDTO.setBlogId(blog.getBlogId());
            updateDTO.setTitle(blog.getTitle());
            updateDTO.setSlug(blog.getSlug());
            updateDTO.setExcerpt(blog.getSummary());
            updateDTO.setContent(blog.getContent());
            updateDTO.setCategoryId(blog.getCategoryId());
            updateDTO.setThumbnailImage(blog.getThumbnailImage());
            updateDTO.setBannerImage(blog.getBannerImage());
            updateDTO.setStatus(blog.getStatus().name());
            updateDTO.setScheduledPublishAt(blog.getScheduledPublishAt());

            model.addAttribute("blog", updateDTO);
            model.addAttribute("categories", categoryService.getActiveCategories());
            model.addAttribute("isEdit", true);
            model.addAttribute("moderator", moderator);
            model.addAttribute("account", moderator); // Sidebar needs this

            return "blog/manager/blog-form";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Không tìm thấy blog: " + e.getMessage());
            return "redirect:/moderator/blogs";
        }
    }

    /**
     * Xử lý cập nhật blog - MOD
     * POST /moderator/blogs/edit/{id}
     */
    @PreAuthorize("hasRole('MOD')")
    @PostMapping("/moderator/blogs/edit/{id}")
    public String updateBlog(
            @PathVariable Long id,
            @Valid @ModelAttribute("blog") UpdateBlogDTO dto,
            BindingResult result,
            @RequestParam(required = false) MultipartFile thumbnailFile,
            @RequestParam(required = false) MultipartFile bannerFile,
            HttpSession session,
            Model model,
            RedirectAttributes ra) {

        Account moderator = (Account) session.getAttribute("loggedInAccount");
        if (moderator == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.getActiveCategories());
            model.addAttribute("isEdit", true);
            model.addAttribute("moderator", moderator);
            model.addAttribute("account", moderator); // Sidebar needs this
            return "blog/manager/blog-form";
        }

        try {
            // Validate blogId matches URL path
            if (dto.getBlogId() != null && !dto.getBlogId().equals(id)) {
                throw new IllegalArgumentException("Blog ID mismatch");
            }
            dto.setBlogId(id);
            
            // Handle thumbnail upload
            if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
                String thumbnailPath = saveUploadedFile(thumbnailFile, "images");
                dto.setThumbnailImage(thumbnailPath);
            }
            
            // Handle banner upload
            if (bannerFile != null && !bannerFile.isEmpty()) {
                String bannerPath = saveUploadedFile(bannerFile, "images");
                dto.setBannerImage(bannerPath);
            }
            
            blogService.updateBlog(dto, moderator.getAccountId());
            ra.addFlashAttribute("success", "Cập nhật blog thành công!");
            return "redirect:/moderator/blogs";
        } catch (Exception e) {
            // Hiển thị thông báo lỗi thân thiện (ví dụ ảnh > 5MB, sai định dạng, ...)
            model.addAttribute("error", e.getMessage());
            model.addAttribute("categories", categoryService.getActiveCategories());
            model.addAttribute("isEdit", true);
            model.addAttribute("moderator", moderator);
            model.addAttribute("account", moderator); // Sidebar needs this
            return "blog/manager/blog-form";
        }
    }

    /**
     * Xóa blog - MOD
     * GET /moderator/blogs/delete/{id}
     */
    @PreAuthorize("hasRole('MOD')")
    @GetMapping("/moderator/blogs/delete/{id}")
    public String deleteBlog(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes ra) {

        Account moderator = (Account) session.getAttribute("loggedInAccount");
        if (moderator == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        try {
            blogService.deleteBlog(id, moderator.getAccountId());
            ra.addFlashAttribute("success", "Xóa blog thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }

        return "redirect:/moderator/blogs";
    }

    /**
     * Xuất bản blog - MOD
     * GET /moderator/blogs/publish/{id}
     */
    @PreAuthorize("hasRole('MOD')")
    @GetMapping("/moderator/blogs/publish/{id}")
    public String publishBlog(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes ra) {

        Account moderator = (Account) session.getAttribute("loggedInAccount");
        if (moderator == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        try {
            blogService.publishBlog(id, moderator.getAccountId());
            ra.addFlashAttribute("success", "Xuất bản blog thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }

        return "redirect:/moderator/blogs";
    }

    /**
     * Helper method để lưu file upload
     */
    private String saveUploadedFile(MultipartFile file, String folderName) throws IOException {
        // Validate file
        if (file.getSize() > 5 * 1024 * 1024) { // 5MB
            throw new IOException("Kích thước ảnh tối đa 5MB. Vui lòng chọn file nhỏ hơn.");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IOException("Chỉ được phép upload file ảnh (JPG, PNG, GIF).");
        }
        
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueFilename = System.currentTimeMillis() + "_" + UUID.randomUUID().toString() + extension;
        
        // Create directory if not exists
        Path uploadPath = Paths.get("uploads", folderName);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Save file
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        // Return web-accessible path
        return "/uploads/" + folderName + "/" + uniqueFilename;
    }

    /**
     * Hủy xuất bản blog - MOD
     * GET /moderator/blogs/unpublish/{id}
     */
    @PreAuthorize("hasRole('MOD')")
    @GetMapping("/moderator/blogs/unpublish/{id}")
    public String unpublishBlog(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes ra) {

        Account moderator = (Account) session.getAttribute("loggedInAccount");
        if (moderator == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        try {
            blogService.unpublishBlog(id, moderator.getAccountId());
            ra.addFlashAttribute("success", "Hủy xuất bản blog thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }

        return "redirect:/moderator/blogs";
    }
}
