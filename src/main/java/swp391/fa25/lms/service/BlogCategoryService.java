package swp391.fa25.lms.service;

import swp391.fa25.lms.dto.blog.BlogCategoryDTO;
import swp391.fa25.lms.dto.blog.CreateBlogCategoryDTO;
import swp391.fa25.lms.dto.blog.UpdateBlogCategoryDTO;
import swp391.fa25.lms.model.BlogCategory;

import java.util.List;

public interface BlogCategoryService {

    /**
     * Tạo category mới
     */
    BlogCategoryDTO createCategory(CreateBlogCategoryDTO dto);

    /**
     * Cập nhật category
     */
    BlogCategoryDTO updateCategory(UpdateBlogCategoryDTO dto);

    /**
     * Xóa category (chỉ nếu không có blog nào)
     */
    void deleteCategory(Long categoryId);

    /**
     * Lấy category theo ID
     */
    BlogCategoryDTO getCategoryById(Long categoryId);

    /**
     * Lấy category theo slug (SEO-friendly URL)
     */
    BlogCategoryDTO getCategoryBySlug(String slug);

    /**
     * Lấy tất cả categories
     */
    List<BlogCategoryDTO> getAllCategories();

    /**
     * Lấy tất cả categories ACTIVE
     */
    List<BlogCategoryDTO> getActiveCategories();

    /**
     * Activate category
     */
    BlogCategoryDTO activateCategory(Long categoryId);

    /**
     * Deactivate category
     */
    BlogCategoryDTO deactivateCategory(Long categoryId);
}
