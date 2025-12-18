package swp391.fa25.lms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.*;
import swp391.fa25.lms.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ToolListService {

    @Autowired
    private ToolListRepository toolListRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private FeedbackRepository feedbackRepository;

    // ======================= USER VIEW (PAGINATED) =======================
    public List<Tool> getToolsForUserPaginated(String keyword,
                                               Long categoryId,
                                               Long authorId,
                                               Tool.LoginMethod loginMethod,
                                               Integer priceMin,
                                               Integer priceMax,
                                               String sort,
                                               int page,
                                               int size) {

        List<Tool> base = toolListRepository.findByStatus(Tool.Status.PUBLISHED);
        enrichToolPrices(base);
        enrichToolRatings(base);

        List<Tool> filtered = filterTools(
                base,
                keyword,
                categoryId,
                authorId,
                loginMethod,
                null,
                priceMin,
                priceMax,
                false
        );

        List<Tool> sorted = sortTools(filtered, sort);
        return paginate(sorted, page, size);
    }

    public int countToolsForUser(String keyword,
                                 Long categoryId,
                                 Long authorId,
                                 Tool.LoginMethod loginMethod,
                                 Integer priceMin,
                                 Integer priceMax) {

        List<Tool> base = toolListRepository.findByStatus(Tool.Status.PUBLISHED);
        enrichToolPrices(base);

        return filterTools(
                base,
                keyword,
                categoryId,
                authorId,
                loginMethod,
                null,
                priceMin,
                priceMax,
                false
        ).size();
    }


    // ======================= SELLER VIEW (PAGINATED) =======================
    public List<Tool> getToolsForSellerPaginated(Long sellerId,
                                                 String keyword,
                                                 Long categoryId,
                                                 Tool.LoginMethod loginMethod,
                                                 Tool.Status status,
                                                 Integer priceMin,
                                                 Integer priceMax,
                                                 String sort,
                                                 int page,
                                                 int size) {

        List<Tool> base = toolListRepository.findBySeller_AccountId(sellerId);
        enrichToolPrices(base);
        enrichToolRatings(base);

        List<Tool> filtered = filterTools(
                base,
                keyword,
                categoryId,
                null,
                loginMethod,
                status,
                priceMin,
                priceMax,
                true
        );

        List<Tool> sorted = sortTools(filtered, sort);
        return paginate(sorted, page, size);
    }

    public int countToolsForSeller(Long sellerId,
                                   String keyword,
                                   Long categoryId,
                                   Tool.LoginMethod loginMethod,
                                   Tool.Status status,
                                   Integer priceMin,
                                   Integer priceMax) {

        List<Tool> base = toolListRepository.findBySeller_AccountId(sellerId);
        enrichToolPrices(base);

        return filterTools(
                base,
                keyword,
                categoryId,
                null,
                loginMethod,
                status,
                priceMin,
                priceMax,
                true
        ).size();
    }


    // ======================= PAGINATION =======================
    private List<Tool> paginate(List<Tool> tools, int page, int size) {
        int from = (page - 1) * size;
        int to = Math.min(from + size, tools.size());
        if (from >= tools.size()) return Collections.emptyList();
        return tools.subList(from, to);
    }


    // ======================= ENRICH PRICE =======================
    private void enrichToolPrices(List<Tool> tools) {
        if (tools == null) return;

        for (Tool t : tools) {
            List<License> licenses = t.getLicenses();
            if (licenses == null || licenses.isEmpty()) {
                t.setMinPrice(null);
                t.setMaxPrice(null);
                continue;
            }

            Double min = null;
            Double max = null;

            for (License lic : licenses) {
                if (lic == null || lic.getPrice() == null) continue;
                double p = lic.getPrice();

                if (min == null || p < min) min = p;
                if (max == null || p > max) max = p;
            }

            t.setMinPrice(min == null ? null : BigDecimal.valueOf(min));
            t.setMaxPrice(max == null ? null : BigDecimal.valueOf(max));
        }
    }

    // ======================= ENRICH RATING =======================
    private void enrichToolRatings(List<Tool> tools) {
        if (tools == null) return;

        for (Tool tool : tools) {
            // Tính trung bình rating từ feedback có status PUBLISHED
            Double avgRating = feedbackRepository.avgRatingByToolAndStatus(tool, Feedback.Status.PUBLISHED);
            Long totalReviews = feedbackRepository.countByToolAndStatus(tool, Feedback.Status.PUBLISHED);

            tool.setAverageRating(avgRating != null ? avgRating : 0.0);
            tool.setTotalReviews(totalReviews);
        }
    }

    // ======================= FILTER CORE =======================
    private List<Tool> filterTools(List<Tool> tools,
                                   String keyword,
                                   Long categoryId,
                                   Long authorId,
                                   Tool.LoginMethod loginMethod,
                                   Tool.Status status,
                                   Integer priceMin,
                                   Integer priceMax,
                                   boolean isSellerView) {

        String kw = (keyword == null) ? null : keyword.toLowerCase(Locale.ROOT).trim();

        return tools.stream()

                .filter(t -> kw == null || kw.isBlank() ||
                        (t.getToolName() != null && t.getToolName().toLowerCase().contains(kw)) ||
                        (t.getDescription() != null && t.getDescription().toLowerCase().contains(kw)))

                .filter(t -> categoryId == null ||
                        (t.getCategory() != null && Objects.equals(t.getCategory().getCategoryId(), categoryId)))

                .filter(t -> isSellerView || authorId == null ||
                        (t.getSeller() != null && Objects.equals(t.getSeller().getAccountId(), authorId)))

                .filter(t -> loginMethod == null || t.getLoginMethod() == loginMethod)

                .filter(t -> status == null || t.getStatus() == status)

                .filter(t -> {
                    if (priceMin == null && priceMax == null) return true;
                    BigDecimal minPrice = t.getMinPrice();
                    if (minPrice == null) return false;
                    double p = minPrice.doubleValue();
                    return (priceMin == null || p >= priceMin) &&
                            (priceMax == null || p <= priceMax);
                })
                .collect(Collectors.toList());
    }

    // ======================= SORT =======================
    private List<Tool> sortTools(List<Tool> tools, String sort) {
        if (sort == null || sort.isBlank()) return tools;

        Comparator<Tool> comparator = null;

        switch (sort) {
            case "date_desc":
                comparator = Comparator.comparing(
                        Tool::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())
                ).reversed();
                break;

            case "date_asc":
                comparator = Comparator.comparing(
                        Tool::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())
                );
                break;

            case "price_asc":
                comparator = Comparator.comparing(
                        t -> t.getMinPrice() == null ? 0.0 : t.getMinPrice().doubleValue()
                );
                break;

            case "price_desc":
                comparator = Comparator.comparing(
                        (Tool t) -> t.getMinPrice() == null ? 0.0 : t.getMinPrice().doubleValue()
                ).reversed();
                break;
        }

        if (comparator == null) return tools;
        return tools.stream().sorted(comparator).collect(Collectors.toList());
    }

    // ======================= DYNAMIC FILTER (LẤY FULL DB) =======================
    public List<Category> getAllCategoriesFromDB() {
        return categoryRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Category::getCategoryName))
                .collect(Collectors.toList());
    }

    public List<Account> getAllSellersFromDB() {
        return accountRepository.findAllSellers()
                .stream()
                .sorted(Comparator.comparing(Account::getFullName))
                .collect(Collectors.toList());
    }


    public List<Tool.Status> getAllStatuses() {
        return Arrays.asList(Tool.Status.values());
    }

    public List<Tool.LoginMethod> getAllLoginMethods() {
        return Arrays.asList(Tool.LoginMethod.values());
    }


    // ======================= SELLER ACTIONS =======================
    public Tool addTool(Tool tool, Long sellerId) {
        Account seller = accountRepository.findById(sellerId)
                .orElseThrow(() -> new IllegalArgumentException("Seller không tồn tại"));

        tool.setSeller(seller);
        tool.setStatus(Tool.Status.PENDING);
        tool.setCreatedAt(LocalDateTime.now());

        if (tool.getQuantity() == null) tool.setQuantity(0);
        if (tool.getAvailableQuantity() == null) tool.setAvailableQuantity(tool.getQuantity());

        return toolListRepository.save(tool);
    }

    public Tool toggleStatus(Long toolId, Long sellerId) {
        Tool tool = toolListRepository.findByToolIdAndSeller_AccountId(toolId, sellerId)
                .orElseThrow(() -> new IllegalArgumentException("Tool không tồn tại hoặc không thuộc seller"));

        if (tool.getStatus() == Tool.Status.PUBLISHED) {
            tool.setStatus(Tool.Status.DEACTIVATED);
        } else if (tool.getStatus() == Tool.Status.DEACTIVATED || tool.getStatus() == Tool.Status.PENDING) {
            tool.setStatus(Tool.Status.PUBLISHED);
        }

        tool.setUpdatedAt(LocalDateTime.now());
        return toolListRepository.save(tool);
    }

    public Tool updateTool(Tool updatedTool, Long sellerId) {
        Tool existing = toolListRepository.findByToolIdAndSeller_AccountId(
                        updatedTool.getToolId(), sellerId)
                .orElseThrow(() -> new IllegalArgumentException("Tool không tồn tại hoặc không thuộc seller"));

        existing.setToolName(updatedTool.getToolName());
        existing.setDescription(updatedTool.getDescription());
        existing.setImage(updatedTool.getImage());
        existing.setCategory(updatedTool.getCategory());
        existing.setLoginMethod(updatedTool.getLoginMethod());
        existing.setNote(updatedTool.getNote());
        existing.setQuantity(updatedTool.getQuantity());
        existing.setAvailableQuantity(updatedTool.getAvailableQuantity());

        existing.setStatus(Tool.Status.PENDING);
        existing.setUpdatedAt(LocalDateTime.now());

        return toolListRepository.save(existing);
    }
}
