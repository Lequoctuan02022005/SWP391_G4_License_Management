package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swp391.fa25.lms.model.Tool;

import java.util.List;
import java.util.Optional;

public interface ToolListRepository extends JpaRepository<Tool, Long> {

    // User: xem tất cả tool đã PUBLISHED
    List<Tool> findByStatus(Tool.Status status);

    // Seller: xem tool của chính mình (mọi trạng thái)
    List<Tool> findBySeller_AccountId(Long accountId);

    // Seller: thao tác trên tool của chính họ (toggle, edit)
    Optional<Tool> findByToolIdAndSeller_AccountId(Long toolId, Long accountId);
}
