package swp391.fa25.lms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Category;
import swp391.fa25.lms.model.License;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.repository.AccountRepository;
import swp391.fa25.lms.repository.ToolListRepository;

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

    // ======================= USER VIEW =======================
    public List<Tool> getToolsForUser(String keyword,
                                      Long categoryId,
                                      Long authorId,
                                      Tool.LoginMethod loginMethod,
                                      Integer priceMin,
                                      Integer priceMax,
                                      String sort) {

        // User chỉ xem tool PUBLISHED
        List<Tool> base = toolListRepository.findByStatus(Tool.Status.PUBLISHED);

        // Tính minPrice, maxPrice theo license
        enrichToolPrices(base);

        List<Tool> filtered = filterTools(
                base,
                keyword,
                categoryId,
                authorId,
                loginMethod,
                null,          // user không lọc theo status
                priceMin,
                priceMax,
                false          // isSellerView = false
        );

        return sortTools(filtered, sort);
    }

    // ======================= SELLER VIEW =======================
    public List<Tool> getToolsForSeller(Long sellerId,
                                        String keyword,
                                        Long categoryId,
                                        Tool.LoginMethod loginMethod,
                                        Tool.Status status,
                                        Integer priceMin,
                                        Integer priceMax,
                                        String sort) {

        // Seller xem tool của chính mình (mọi trạng thái)
        List<Tool> base = toolListRepository.findBySeller_AccountId(sellerId);

        // Tính minPrice, maxPrice theo license
        enrichToolPrices(base);

        // seller đã bị cố định theo sellerId, không cần authorId filter
        List<Tool> filtered = filterTools(
                base,
                keyword,
                categoryId,
                null,           // authorId = null
                loginMethod,
                status,
                priceMin,
                priceMax,
                true            // isSellerView = true
        );

        return sortTools(filtered, sort);
    }

    // ======================= ENRICH PRICE FROM LICENSES =======================
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

            if (min == null) {
                t.setMinPrice(null);
                t.setMaxPrice(null);
            } else {
                t.setMinPrice(BigDecimal.valueOf(min));
                t.setMaxPrice(BigDecimal.valueOf(max));
            }
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
                // keyword: name + description
                .filter(t -> kw == null || kw.isBlank()
                        || (t.getToolName() != null
                        && t.getToolName().toLowerCase(Locale.ROOT).contains(kw))
                        || (t.getDescription() != null
                        && t.getDescription().toLowerCase(Locale.ROOT).contains(kw)))
                // category
                .filter(t -> categoryId == null
                        || (t.getCategory() != null
                        && Objects.equals(t.getCategory().getCategoryId(), categoryId)))
                // author: chỉ dùng cho user view (isSellerView = false)
                .filter(t -> isSellerView || authorId == null
                        || (t.getSeller() != null
                        && Objects.equals(t.getSeller().getAccountId(), authorId)))
                // login method
                .filter(t -> loginMethod == null || t.getLoginMethod() == loginMethod)
                // status: chỉ dùng cho seller view
                .filter(t -> status == null || t.getStatus() == status)
                // price range: dùng minPrice (giá thấp nhất của license)
                .filter(t -> {
                    if (priceMin == null && priceMax == null) return true;
                    BigDecimal minPrice = t.getMinPrice();
                    if (minPrice == null) return false;

                    double p = minPrice.doubleValue();
                    return (priceMin == null || p >= priceMin)
                            && (priceMax == null || p <= priceMax);
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
                        Tool::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).reversed();
                break;

            case "date_asc":
                comparator = Comparator.comparing(
                        Tool::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                );
                break;

            case "price_asc":
                comparator = Comparator.comparing(
                        t -> {
                            BigDecimal minPrice = t.getMinPrice();
                            return (minPrice == null) ? 0.0 : minPrice.doubleValue();
                        }
                );
                break;

            case "price_desc":
                comparator = Comparator.comparing(
                        (Tool t) -> {
                            BigDecimal minPrice = t.getMinPrice();
                            return (minPrice == null) ? 0.0 : minPrice.doubleValue();
                        }
                ).reversed();
                break;

            default:
                // giữ nguyên order
                break;
        }

        if (comparator == null) return tools;

        return tools.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    // ======================= SELLER ACTIONS =======================
    public Tool addTool(Tool tool, Long sellerId) {
        Account seller = accountRepository.findById(sellerId)
                .orElseThrow(() -> new IllegalArgumentException("Seller không tồn tại"));

        tool.setSeller(seller);
        tool.setStatus(Tool.Status.PENDING); // default: chờ duyệt
        tool.setCreatedAt(LocalDateTime.now());

        if (tool.getQuantity() == null) {
            tool.setQuantity(0);
        }
        if (tool.getAvailableQuantity() == null) {
            tool.setAvailableQuantity(tool.getQuantity());
        }

        return toolListRepository.save(tool);
    }

    public Tool toggleStatus(Long toolId, Long sellerId) {
        Tool tool = toolListRepository.findByToolIdAndSeller_AccountId(toolId, sellerId)
                .orElseThrow(() -> new IllegalArgumentException("Tool không tồn tại hoặc không thuộc seller"));

        if (tool.getStatus() == Tool.Status.PUBLISHED) {
            tool.setStatus(Tool.Status.DEACTIVATED);
        } else if (tool.getStatus() == Tool.Status.DEACTIVATED
                || tool.getStatus() == Tool.Status.PENDING) {
            tool.setStatus(Tool.Status.PUBLISHED);
        } else {
            // các trạng thái khác bỏ qua
            return tool;
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

        existing.setStatus(Tool.Status.PENDING); // sau khi sửa quay về PENDING
        existing.setUpdatedAt(LocalDateTime.now());

        return toolListRepository.save(existing);
    }

    // ======================= DYNAMIC FILTER VALUES =======================
    public List<Category> getAvailableCategories(List<Tool> tools) {
        return tools.stream()
                .map(Tool::getCategory)
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Comparator.comparing(Category::getCategoryName))
                .collect(Collectors.toList());
    }

    public List<Account> getAvailableSellers(List<Tool> tools) {
        return tools.stream()
                .map(Tool::getSeller)
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Comparator.comparing(Account::getFullName))
                .collect(Collectors.toList());
    }

    public List<Tool.Status> getAvailableStatuses(List<Tool> tools) {
        return tools.stream()
                .map(Tool::getStatus)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    public List<Tool.LoginMethod> getAvailableLoginMethods(List<Tool> tools) {
        return tools.stream()
                .map(Tool::getLoginMethod)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
}
