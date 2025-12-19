package swp391.fa25.lms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.fa25.lms.dto.blog.*;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Blog;
import swp391.fa25.lms.model.BlogCategory;
import swp391.fa25.lms.model.Role;
import swp391.fa25.lms.repository.AccountRepository;
import swp391.fa25.lms.repository.BlogCategoryRepository;
import swp391.fa25.lms.repository.BlogRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlogServiceImpl implements BlogService {

    private final BlogRepository blogRepository;
    private final BlogCategoryRepository categoryRepository;
    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public BlogDetailDTO createBlog(CreateBlogDTO dto, Long authorId) {
        log.info("Creating blog: {} by author: {}", dto.getTitle(), authorId);

        // Validate author exists and is MOD or MANAGER
        Account author = accountRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("Author not found"));
        
        // Check role - getRoleName() returns RoleName enum, not String
        // Allow MOD and MANAGER to create blogs
        if (author.getRole() == null || 
            (author.getRole().getRoleName() != Role.RoleName.MOD && 
             author.getRole().getRoleName() != Role.RoleName.MANAGER)) {
            String roleStr = author.getRole() != null ? author.getRole().getRoleName().toString() : "NULL";
            throw new RuntimeException("Only MOD or MANAGER can create blogs. Your role: " + roleStr);
        }

        // Validate category exists
        BlogCategory category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        // Create blog entity
        Blog blog = new Blog();
        blog.setTitle(dto.getTitle());
        // Use excerpt field (form binds to excerpt, not summary)
        blog.setSummary(dto.getExcerpt());
        blog.setContent(dto.getContent());
        blog.setCategory(category);
        blog.setAuthor(author);
        blog.setThumbnailImage(dto.getThumbnailImage());
        blog.setBannerImage(dto.getBannerImage());
        blog.setScheduledPublishAt(dto.getScheduledPublishAt());

        // Set status
        try {
            blog.setStatus(Blog.Status.valueOf(dto.getStatus().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid status: " + dto.getStatus());
        }

        // Generate unique slug trước khi save (để đảm bảo không trùng)
        String uniqueSlug = generateUniqueSlug(dto.getTitle());
        blog.setSlug(uniqueSlug);

        Blog savedBlog = blogRepository.save(blog);
        log.info("Blog created successfully with ID: {}", savedBlog.getBlogId());

        return new BlogDetailDTO(savedBlog);
    }

    @Override
    @Transactional
    public BlogDetailDTO updateBlog(UpdateBlogDTO dto, Long authorId) {
        log.info("Updating blog ID: {} by author: {}", dto.getBlogId(), authorId);

        // Tìm blog cần update
        Blog blog = blogRepository.findById(dto.getBlogId())
                .orElseThrow(() -> new RuntimeException("Blog not found"));

        // Kiểm tra quyền: chỉ author (MOD hoặc MANAGER) mới được update
        if (!blog.getAuthor().getAccountId().equals(authorId)) {
            throw new RuntimeException("Only the author can update this blog");
        }

        // Validate category exists
        BlogCategory category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        // Update các fields
        blog.setTitle(dto.getTitle());
        blog.setSummary(dto.getExcerpt() != null ? dto.getExcerpt() : dto.getSummary());
        blog.setContent(dto.getContent());
        blog.setCategory(category);
        blog.setThumbnailImage(dto.getThumbnailImage());
        blog.setBannerImage(dto.getBannerImage());
        blog.setScheduledPublishAt(dto.getScheduledPublishAt());

        // Update status
        try {
            blog.setStatus(Blog.Status.valueOf(dto.getStatus().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid status: " + dto.getStatus());
        }

        // Update slug nếu có
        if (dto.getSlug() != null && !dto.getSlug().trim().isEmpty()) {
            String newSlug = Blog.toSlug(dto.getSlug().trim());
            if (!newSlug.equals(blog.getSlug())) {
                // Kiểm tra slug unique
                Optional<Blog> existing = blogRepository.findBySlug(newSlug);
                if (existing.isPresent() && !existing.get().getBlogId().equals(blog.getBlogId())) {
                    // Slug đã tồn tại, generate unique
                    blog.setSlug(generateUniqueSlug(newSlug));
                } else {
                    blog.setSlug(newSlug);
                }
            }
        }

        Blog updatedBlog = blogRepository.save(blog);
        log.info("Blog updated successfully: {}", updatedBlog.getBlogId());

        return new BlogDetailDTO(updatedBlog);
    }

    @Override
    @Transactional
    public void deleteBlog(Long blogId, Long authorId) {
        log.info("Soft deleting blog ID: {} by author: {}", blogId, authorId);

        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new RuntimeException("Blog not found"));

        // Check authorization: only author (MOD or MANAGER) can delete
        if (!blog.getAuthor().getAccountId().equals(authorId)) {
            throw new RuntimeException("You don't have permission to delete this blog. Only the author can delete.");
        }

        blog.archive();
        blogRepository.save(blog);
        log.info("Blog archived successfully: {}", blogId);
    }

    @Override
    @Transactional(readOnly = true)
    public BlogDetailDTO getBlogById(Long blogId) {
        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new RuntimeException("Blog not found"));
        return new BlogDetailDTO(blog);
    }

    @Override
    @Transactional
    public BlogDetailDTO getBlogBySlugAndIncrementView(String slug) {
        Blog blog = blogRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Blog not found"));

        // Only increment view for published blogs
        if (blog.isPublished()) {
            blog.incrementViewCount();
            blogRepository.save(blog);
        }

        BlogDetailDTO detailDTO = new BlogDetailDTO(blog);

        // Load related blogs
        if (blog.getCategory() != null) {
            List<Blog> relatedBlogs = blogRepository.findRelatedBlogs(
                    blog.getCategory().getCategoryId(),
                    blog.getBlogId(),
                    PageRequest.of(0, 5)
            );
            detailDTO.setRelatedBlogs(relatedBlogs.stream()
                    .map(BlogListItemDTO::new)
                    .collect(Collectors.toList()));
        }

        return detailDTO;
    }

    @Override
    @Transactional
    public BlogDetailDTO publishBlog(Long blogId, Long authorId) {
        log.info("Publishing blog ID: {} by author: {}", blogId, authorId);

        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new RuntimeException("Blog not found"));

        // Check authorization: only author (MOD or MANAGER) can publish
        if (!blog.getAuthor().getAccountId().equals(authorId)) {
            throw new RuntimeException("You don't have permission to publish this blog. Only the author can publish.");
        }

        blog.publish();
        Blog publishedBlog = blogRepository.save(blog);
        log.info("Blog published successfully: {}", blogId);

        return new BlogDetailDTO(publishedBlog);
    }

    @Override
    @Transactional
    public BlogDetailDTO unpublishBlog(Long blogId, Long authorId) {
        log.info("Unpublishing blog ID: {} by author: {}", blogId, authorId);

        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new RuntimeException("Blog not found"));

        // Check authorization: only author (MOD or MANAGER) can unpublish
        if (!blog.getAuthor().getAccountId().equals(authorId)) {
            throw new RuntimeException("You don't have permission to unpublish this blog. Only the author can unpublish.");
        }

        blog.unpublish();
        Blog unpublishedBlog = blogRepository.save(blog);
        log.info("Blog unpublished successfully: {}", blogId);

        return new BlogDetailDTO(unpublishedBlog);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BlogListItemDTO> getPublishedBlogs(Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        Page<Blog> blogs = blogRepository.findPublishedBlogs(now, pageable);
        return blogs.map(BlogListItemDTO::new);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BlogListItemDTO> getBlogsByCategory(Long categoryId, Pageable pageable) {
        BlogCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Page<Blog> blogs = blogRepository.findByCategoryAndStatus(
                category,
                Blog.Status.PUBLISHED,
                pageable
        );
        return blogs.map(BlogListItemDTO::new);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BlogListItemDTO> getBlogsByAuthor(Long authorId, Pageable pageable) {
        Page<Blog> blogs = blogRepository.findByAuthorAccountId(authorId, pageable);
        return blogs.map(BlogListItemDTO::new);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<BlogListItemDTO> searchBlogs(String keyword, Pageable pageable) {
        Page<Blog> blogs = blogRepository.searchByKeyword(
                keyword,
                Blog.Status.PUBLISHED,
                pageable
        );
        return blogs.map(BlogListItemDTO::new);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlogListItemDTO> getTopViewedBlogs(int limit) {
        List<Blog> blogs = blogRepository.findTopViewedBlogs(
                PageRequest.of(0, limit)
        );
        return blogs.stream()
                .map(BlogListItemDTO::new)
                .collect(Collectors.toList());
    }

    // Private helper method for generating unique slug
    private String generateUniqueSlug(String title) {
        String baseSlug = Blog.toSlug(title);
        String slug = baseSlug;
        int counter = 1;

        while (blogRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter;
            counter++;
        }

        return slug;
    }
}
