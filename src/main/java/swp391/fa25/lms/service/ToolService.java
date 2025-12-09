package swp391.fa25.lms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.fa25.lms.model.*;
import swp391.fa25.lms.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class ToolService {

    @Autowired private ToolRepository toolRepo;
    @Autowired private CategoryRepository categoryRepo;
    @Autowired private LicenseToolRepository licenseRepo;
    @Autowired private FeedbackRepository feedbackRepo;
    @Autowired private FavoriteService favoriteService;
    @Autowired private LicenseAccountRepository licenseAccountRepository;

    // ========== Finders ==========
    public boolean existsByToolName(String name) {
        return toolRepo.existsByToolName(name);
    }

    public Tool getToolById(Long id) {
        return toolRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tool not found."));
    }

    public Tool getToolByIdAndSeller(Long id, Account seller) {
        return toolRepo.findByToolIdAndSeller(id, seller).orElse(null);
    }

    public List<Category> getAllCategories() {
        return categoryRepo.findAll();
    }

    public Category getCategoryById(Long id) {
        return categoryRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found."));
    }

    // ========== Create Tool ==========
    public Tool createTool(Tool tool, Category category) {
        tool.setCategory(category);
        tool.setStatus(Tool.Status.PENDING);
        tool.setCreatedAt(LocalDateTime.now());
        tool.setUpdatedAt(LocalDateTime.now());
        return toolRepo.save(tool);
    }

    public void createLicensesForTool(Tool tool, List<License> list) {
        for (License l : list) {
            l.setTool(tool);
            l.setCreatedAt(LocalDateTime.now());
            licenseRepo.save(l);
        }
    }

    // ========== Update Tool (USER_PASSWORD Mode) ==========
    public void updateTool(Long id,
                           Tool newData,
                           String imagePath,
                           String toolPath,
                           List<Integer> licenseDays,
                           List<Double> licensePrices,
                           Account seller) {

        Tool tool = getToolById(id);

        // BASIC INFO
        tool.setToolName(newData.getToolName());
        tool.setDescription(newData.getDescription());
        tool.setNote(newData.getNote());
        tool.setUpdatedAt(LocalDateTime.now());
        tool.setStatus(Tool.Status.PENDING);
        tool.setQuantity(newData.getQuantity());
        tool.setAvailableQuantity(newData.getQuantity());

        // CATEGORY
        if (newData.getCategory() != null) {
            Category c = getCategoryById(newData.getCategory().getCategoryId());
            tool.setCategory(c);
        }

        // IMAGE
        if (imagePath != null && !imagePath.isBlank()) {
            tool.setImage(imagePath);
        }

        // TOOL FILE
        if (toolPath != null && !toolPath.isBlank()) {
            ToolFile f = new ToolFile();
            f.setTool(tool);
            f.setFilePath(toolPath);
            f.setFileType(ToolFile.FileType.ORIGINAL);
            f.setUploadedBy(seller);
            f.setCreatedAt(LocalDateTime.now());
            if (tool.getFiles() == null) tool.setFiles(new ArrayList<>());
            tool.getFiles().add(f);
        }

        // LICENSE LIST
        if (licenseDays != null && licensePrices != null && licenseDays.size() == licensePrices.size()) {
            List<License> old = licenseRepo.findByTool_ToolId(id);

            for (int i = 0; i < licenseDays.size(); i++) {
                License lic;

                if (i < old.size()) lic = old.get(i);
                else {
                    lic = new License();
                    lic.setTool(tool);
                    old.add(lic);
                }

                lic.setName("License " + licenseDays.get(i) + " days");
                lic.setDurationDays(licenseDays.get(i));
                lic.setPrice(licensePrices.get(i));

                licenseRepo.save(lic);
            }

            if (old.size() > licenseDays.size()) {
                for (int i = licenseDays.size(); i < old.size(); i++) {
                    licenseRepo.delete(old.get(i));
                }
            }
        }

        toolRepo.save(tool);
    }

    // ========== Update License + Quantity (TOKEN Mode) ==========
    public void updateQuantityAndLicenses(Long toolId, int qty, List<License> newLic) {
        Tool tool = getToolById(toolId);
        List<License> old = licenseRepo.findByTool_ToolId(toolId);

        for (int i = 0; i < newLic.size(); i++) {
            License src = newLic.get(i);
            License target;

            if (i < old.size()) target = old.get(i);
            else {
                target = new License();
                target.setTool(tool);
            }

            target.setName(src.getName());
            target.setDurationDays(src.getDurationDays());
            target.setPrice(src.getPrice());

            licenseRepo.save(target);
        }

        if (old.size() > newLic.size()) {
            for (int i = newLic.size(); i < old.size(); i++) {
                licenseRepo.delete(old.get(i));
            }
        }
        tool.setQuantity(qty);
        int available = licenseAccountRepository.countByLicense_Tool_ToolIdAndUsedFalse(toolId);
        tool.setAvailableQuantity(available);
        tool.setUpdatedAt(LocalDateTime.now());
        toolRepo.save(tool);
    }


    // ================== SEARCH + FILTER (giữ nguyên, chỉ đổi cách tính avg/count) ==================
    public Page<Tool> searchAndFilterTools(String keyword, Long categoryId, String dateFilter,
                                           String priceFilter, Integer ratingFilter,
                                           Account account, int page, int size) {

        List<Tool> tools = toolRepo.findAllPublishedAndSellerActive();

//        List<Tool> tools = toolRepo.findAll();
        // Search keyword (tool name hoặc seller name)
        if (keyword != null && !keyword.isEmpty()) {
            String kwLower = keyword.toLowerCase();
            tools = tools.stream()
                    .filter(t -> t.getToolName().toLowerCase().contains(kwLower)
                            || t.getSeller().getFullName().toLowerCase().contains(kwLower))
                    .toList();
        }

        // Filter theo category
        if (categoryId != null && categoryId > 0) {
            tools = tools.stream()
                    .filter(t -> t.getCategory() != null && t.getCategory().getCategoryId().equals(categoryId))
                    .toList();
        }

        // Filter theo ngày đăng
        if (dateFilter != null && !dateFilter.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            switch (dateFilter) {
                case "1" -> tools = tools.stream().sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())).toList(); // mới nhất
                case "2" -> tools = tools.stream().filter(t -> t.getCreatedAt().isAfter(now.minusDays(30))).toList();
                case "3" -> tools = tools.stream().filter(t -> t.getCreatedAt().isAfter(now.minusMonths(3))).toList();
            }
        }

        // Tính min/max price + avg rating + total reviews (CHỈ tính feedback PUBLISHED hoặc NULL cho tương thích cũ)
        tools.forEach(tool -> {
            // Giá
            if (tool.getLicenses() != null && !tool.getLicenses().isEmpty()) {
                BigDecimal min = tool.getLicenses().stream()
                        .map(l -> BigDecimal.valueOf(l.getPrice()))
                        .min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
                BigDecimal max = tool.getLicenses().stream()
                        .map(l -> BigDecimal.valueOf(l.getPrice()))
                        .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
                tool.setMinPrice(min);
                tool.setMaxPrice(max);
            } else {
                tool.setMinPrice(BigDecimal.ZERO);
                tool.setMaxPrice(BigDecimal.ZERO);
            }

            // Rating & total CHỈ tính PUBLISHED (hoặc status NULL)
            Double avg = feedbackRepo.avgRatingByToolAndStatusOrNull(tool, Feedback.Status.PUBLISHED);
            Long total = feedbackRepo.countByToolAndStatusOrNull(tool, Feedback.Status.PUBLISHED);
            tool.setAverageRating(avg != null ? avg : 0.0);
            tool.setTotalReviews(total != null ? total : 0L);
        });

        // Filter theo price
        if (priceFilter != null && !priceFilter.equals("all")) {
            tools = tools.stream().filter(t -> {
                BigDecimal min = t.getMinPrice() != null ? t.getMinPrice() : BigDecimal.ZERO;
                return switch (priceFilter) {
                    case "under100k" -> min.compareTo(BigDecimal.valueOf(100_000)) < 0;
                    case "100k-500k" -> min.compareTo(BigDecimal.valueOf(100_000)) >= 0
                            && min.compareTo(BigDecimal.valueOf(500_000)) <= 0;
                    case "500k-1m" -> min.compareTo(BigDecimal.valueOf(500_000)) > 0
                            && min.compareTo(BigDecimal.valueOf(1_000_000)) <= 0;
                    case "above1m" -> min.compareTo(BigDecimal.valueOf(1_000_000)) > 0;
                    default -> true;
                };
            }).toList();
        }

        // Filter theo rating
        if (ratingFilter != null && ratingFilter > 0) {
            tools = tools.stream().filter(t -> t.getAverageRating() >= ratingFilter).toList();
        }

        // isFavorite
        if (account != null) {
            Set<Long> favIds = favoriteService.getFavoriteToolIds(account);
            tools.forEach(t -> t.setIsFavorite(favIds.contains(t.getToolId())));
        } else {
            tools.forEach(t -> t.setIsFavorite(false));
        }

        // Pagination
        int start = page * size;
        int end = Math.min(start + size, tools.size());
        List<Tool> pagedList = tools.subList(Math.min(start, tools.size()), end);

        return new PageImpl<>(pagedList, PageRequest.of(page, size), tools.size());
    }

}
