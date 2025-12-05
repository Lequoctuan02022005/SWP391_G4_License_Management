package swp391.fa25.lms.controller.blog;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller for serving Blog view templates (Thymeleaf)
 * Handles page navigation for both public blog pages and manager pages
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/blog")
public class BlogViewController {

    /**
     * Display public blog list page
     * Shows all published blogs with categories, search, and pagination
     */
    @GetMapping({"/", ""})
    public String blogList(Model model) {
        return "blog/blog-list";
    }

    /**
     * Display blog detail page by slug
     * Shows full blog content with related blogs
     */
    @GetMapping("/{slug}")
    public String blogDetail(@PathVariable String slug, Model model) {
        model.addAttribute("slug", slug);
        return "blog/blog-detail";
    }

    /**
     * Display blog manager page (requires MANAGER role)
     * Handles CRUD operations for blogs
     */
    @GetMapping("/manager")
    public String blogManager(Model model) {
        return "blog/blog-manager";
    }

    /**
     * Display category manager page (requires MANAGER role)
     * Handles CRUD operations for blog categories
     */
    @GetMapping("/category-manager")
    public String categoryManager(Model model) {
        return "blog/category-manager";
    }
}
