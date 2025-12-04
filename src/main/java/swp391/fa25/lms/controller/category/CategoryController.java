package swp391.fa25.lms.controller.category;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import swp391.fa25.lms.model.Category;
import swp391.fa25.lms.service.CategoryService;

@Controller
@RequestMapping("/mod/categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    // LIST + SEARCH
    @GetMapping
    public String list(@RequestParam(required = false) String keyword, Model model) {

        if (keyword != null && !keyword.isBlank()) {
            model.addAttribute("categories", categoryService.search(keyword));
        } else {
            model.addAttribute("categories", categoryService.getAll());
        }

        model.addAttribute("keyword", keyword);

        return "mod/category-list";
    }

    // CREATE FORM
    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("category", new Category());
        return "mod/category-create";
    }

    // CREATE ACTION
    @PostMapping("/create")
    public String create(@ModelAttribute Category category) {

        category.setStatus(Category.Status.ACTIVE);

        categoryService.create(category);
        return "redirect:/mod/categories";
    }

    // EDIT FORM
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        Category cat = categoryService.getById(id);
        if (cat == null) {
            return "redirect:/mod/categories";
        }
        model.addAttribute("category", cat);
        return "mod/category-edit";
    }

    // EDIT ACTION
    @PostMapping("/edit/{id}")
    public String update(@PathVariable Long id, @ModelAttribute Category category) {
        categoryService.update(id, category);
        return "redirect:/mod/categories";
    }

    // DELETE
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        categoryService.delete(id);
        return "redirect:/mod/categories";
    }
}
