package swp391.fa25.lms.controller.sellerpackage;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import swp391.fa25.lms.model.SellerPackage;
import swp391.fa25.lms.repository.SellerPackageRepository;
import swp391.fa25.lms.service.SellerPackageService;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/seller-packages")
@PreAuthorize("hasRole('MANAGER')")
public class SellerPackageManagementController {

    private final SellerPackageService sellerPackageService;

    // ================= LIST =================
    @GetMapping
    public String list(
            @RequestParam(required = false) String packageName,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Integer minDuration,
            @RequestParam(required = false) Integer maxDuration,
            @RequestParam(required = false) SellerPackage.Status status,
            Pageable pageable,
            Model model
    ) {
        Page<SellerPackage> page =
                sellerPackageService.filterPackages(
                        packageName,
                        minPrice,
                        maxPrice,
                        minDuration,
                        maxDuration,
                        status,
                        pageable
                );

        model.addAttribute("page", page);
        model.addAttribute("packages", page.getContent());
        model.addAttribute("statusList", SellerPackage.Status.values());

        return "sellerpackage/package-list";
    }

    // ================= CREATE =================
    @GetMapping("/create")
    public String create(Model model) {
        model.addAttribute("pkg", new SellerPackage());
        model.addAttribute("statusList", SellerPackage.Status.values());
        return "sellerpackage/package-form";
    }

    // ================= EDIT =================
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable int id, Model model) {
        SellerPackage pkg = sellerPackageService.getById(id);
        model.addAttribute("pkg", pkg);
        model.addAttribute("statusList", SellerPackage.Status.values());
        return "sellerpackage/package-form";
    }

    // ================= SAVE =================
    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("pkg") SellerPackage pkg,
                       BindingResult result,
                       Model model) {

        if (result.hasErrors()) {
            model.addAttribute("statusList", SellerPackage.Status.values());
            return "sellerpackage/package-form";
        }

        sellerPackageService.save(pkg);
        return "redirect:/seller-packages";
    }
}