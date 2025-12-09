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
     * Tìm kiếm và sắp xếp categories
     * @param keyword - search keyword for name, description, slug
     * @param status - status filter: "ACTIVE", "INACTIVE", or null for all
     * @param sortBy - sort field: displayOrder, categoryName, createdAt, status
     */
    List<BlogCategoryDTO> searchCategories(String keyword, String status, String sortBy);

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
