package swp391.fa25.lms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.repository.AccountRepository;
import swp391.fa25.lms.repository.ToolListRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class ToolListService {

    @Autowired
    private ToolListRepository toolListRepository;

    @Autowired
    private AccountRepository accountRepository;

    // ======================= USER VIEW =======================

    public List<Tool> getToolsForUser(String keyword,
                                      Tool.LoginMethod loginMethod) {

        // User chỉ xem tool PUBLISHED
        List<Tool> base = toolListRepository.findByStatus(Tool.Status.PUBLISHED);
        return filterTools(base, keyword, loginMethod, null);
    }

    // ======================= SELLER VIEW =======================

    public List<Tool> getToolsForSeller(Long sellerId,
                                        String keyword,
                                        Tool.LoginMethod loginMethod,
                                        Tool.Status status) {

        List<Tool> base = toolListRepository.findBySeller_AccountId(sellerId);
        return filterTools(base, keyword, loginMethod, status);
    }

    private List<Tool> filterTools(List<Tool> tools,
                                   String keyword,
                                   Tool.LoginMethod loginMethod,
                                   Tool.Status status) {

        String kw = (keyword == null) ? null : keyword.toLowerCase(Locale.ROOT).trim();

        return tools.stream()
                .filter(t -> kw == null || kw.isBlank()
                        || (t.getToolName() != null && t.getToolName().toLowerCase(Locale.ROOT).contains(kw))
                        || (t.getDescription() != null && t.getDescription().toLowerCase(Locale.ROOT).contains(kw)))
                .filter(t -> loginMethod == null || t.getLoginMethod() == loginMethod)
                .filter(t -> status == null || t.getStatus() == status)
                .collect(Collectors.toList());
    }

    // ======================= SELLER ACTIONS =======================

    public Tool addTool(Tool tool, Long sellerId) {
        Account seller = accountRepository.findById(sellerId)
                .orElseThrow(() -> new IllegalArgumentException("Seller không tồn tại"));

        tool.setSeller(seller);
        tool.setStatus(Tool.Status.PENDING);       // Default: chờ MOD duyệt
        tool.setCreatedAt(LocalDateTime.now());

        // quantity, availableQuantity nếu null thì set 0
        if (tool.getQuantity() == null) {
            tool.setQuantity(0);
        }
        if (tool.getAvailableQuantity() == null) {
            tool.setAvailableQuantity(tool.getQuantity());
        }

        return toolListRepository.save(tool);
    }

    /**
     * Seller toggle PUBLISHED <-> DEACTIVATED cho tool của chính mình
     */
    public Tool toggleStatus(Long toolId, Long sellerId) {
        Tool tool = toolListRepository.findByToolIdAndSeller_AccountId(toolId, sellerId)
                .orElseThrow(() -> new IllegalArgumentException("Tool không tồn tại hoặc không thuộc seller"));

        if (tool.getStatus() == Tool.Status.PUBLISHED) {
            tool.setStatus(Tool.Status.DEACTIVATED);
        } else if (tool.getStatus() == Tool.Status.DEACTIVATED) {
            tool.setStatus(Tool.Status.PUBLISHED);
        } else {
            // Các trạng thái khác thì bỏ qua (PENDING / APPROVED / REJECTED)
            return tool;
        }

        tool.setUpdatedAt(LocalDateTime.now());
        return toolListRepository.save(tool);
    }

    /**
     * Seller chỉnh sửa tool: sau chỉnh sửa -> quay về PENDING để MOD duyệt lại
     */
    public Tool updateTool(Tool updatedTool, Long sellerId) {
        Tool existing = toolListRepository.findByToolIdAndSeller_AccountId(updatedTool.getToolId(), sellerId)
                .orElseThrow(() -> new IllegalArgumentException("Tool không tồn tại hoặc không thuộc seller"));

        existing.setToolName(updatedTool.getToolName());
        existing.setDescription(updatedTool.getDescription());
        existing.setImage(updatedTool.getImage());
        existing.setCategory(updatedTool.getCategory());
        existing.setLoginMethod(updatedTool.getLoginMethod());
        existing.setNote(updatedTool.getNote());
        existing.setQuantity(updatedTool.getQuantity());
        existing.setAvailableQuantity(updatedTool.getAvailableQuantity());

        existing.setStatus(Tool.Status.PENDING);        // Sau edit: quay lại pending
        existing.setUpdatedAt(LocalDateTime.now());

        return toolListRepository.save(existing);
    }
}
