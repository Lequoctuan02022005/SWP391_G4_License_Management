package swp391.fa25.lms.controller.common;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import swp391.fa25.lms.service.CategoryService;

@Controller
@RequiredArgsConstructor
@RequestMapping({"/", "/home"})
public class HomeController {

    private final CategoryService categoryService;
    @GetMapping("/")
    public String root() {
        return "redirect:/home";   // Điều hướng tới localhost:7070 cũng vào home
    }
    @GetMapping
    public String home(Model model) {
        // Giá trị mặc định cho form

        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("keyword", "");
        model.addAttribute("selectedCategory", null);

        return "common/home";
    }
}