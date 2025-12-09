package swp391.fa25.lms.controller.mod;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import swp391.fa25.lms.model.Category;
import swp391.fa25.lms.service.CategoryService;

import java.util.List;

@Controller
@PreAuthorize("hasRole('MOD')")
@RequestMapping("/moderator")
public class ModeratorController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/dashboard")
    public String dashboard() {
        return "moderator/dashboard";
    }

    @GetMapping("/categories")
    public String listCategories(@RequestParam(required = false) String keyword,
                                 Model model) {

        List<Category> result;

        if (keyword != null && !keyword.isBlank()) {
            result = categoryService.search(keyword);
        } else {
            result = categoryService.getAll();
        }

        model.addAttribute("categories", result);
        model.addAttribute("keyword", keyword);

        return "moderator/category-list";
    }

    // CREATE FORM (GET)
    @GetMapping("/categories/create")
    public String showCreateForm(Model model) {
        model.addAttribute("category", new Category());
        model.addAttribute("statusValues", Category.Status.values());
        return "moderator/category-create";
    }

    // CREATE (POST)
    @PostMapping("/categories/create")
    public String create(@ModelAttribute("category") Category category) {

        categoryService.create(category);

        return "redirect:/moderator/categories";
    }

    // EDIT FORM (GET)
    @GetMapping("/categories/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {

        Category category = categoryService.getById(id);

        if (category == null) {
            return "redirect:/moderator/categories";
        }

        model.addAttribute("category", category);
        model.addAttribute("statusValues", Category.Status.values());

        return "moderator/category-edit";
    }

    // EDIT (POST)
    @PostMapping("/categories/edit/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute("category") Category category) {

        categoryService.update(id, category);

        return "redirect:/moderator/categories";
    }

    // DELETE CATEGORY
    @GetMapping("/categories/delete/{id}")
    public String delete(@PathVariable Long id) {
        categoryService.delete(id);
        return "redirect:/moderator/categories";
    }
}
