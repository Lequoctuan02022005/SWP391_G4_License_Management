package swp391.fa25.lms.controller.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Feedback;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.service.CategoryService;
import swp391.fa25.lms.service.LicenseAccountService;
import swp391.fa25.lms.service.ToolService;
import jakarta.servlet.http.HttpServletRequest;   // THÊM DÒNG NÀY VÀO ĐẦU FILE
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Controller
public class ToolCommonController {
    @Autowired
    private ToolService toolService;

    @Autowired
    private CategoryService categoryService;


    @Autowired
    private LicenseAccountService licenseAccountService;

    /**
     * Hiển thị trang detail tool
     */
    @GetMapping("/tools/{id}")
    public String showToolDetail(@PathVariable("id") Long id,
                                 @RequestParam(value = "reviewPage", defaultValue = "0") int reviewPage,
                                 Model model , HttpServletRequest request) {

        // Lấy tool theo id
        Optional<Tool> maybeTool = toolService.findPublishedToolById(id);
        if (maybeTool.isEmpty()) {
            model.addAttribute("errorMessage", "Không tìm thấy sản phẩm hoặc sản phẩm chưa công khai.");
            return "public/404"; // 404.html
        }

        Tool tool = maybeTool.get();
        System.out.println("Detail Tool ID: " + tool.getToolId() + ", Image path: '" + tool.getImage() + "'");
        System.out.println("đã mua : " + licenseAccountService.getActiveLicenses(tool.getToolId()).size());
        tool.setAvailableQuantity(tool.getQuantity() - licenseAccountService.getActiveLicenses(tool.getToolId()).size());

        Account account = (Account) request.getSession().getAttribute("loggedInAccount");
        // Data view
        model.addAttribute("tool", tool);
        model.addAttribute("account", account);


        return "common/tool-detail";
    }
}
