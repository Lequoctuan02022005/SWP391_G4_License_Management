package swp391.fa25.lms.repository;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Tool;

import java.util.Optional;

@Repository
public interface ToolRepository extends JpaRepository<Tool, Long> {
    Optional<Tool> findByToolIdAndSeller(Long toolId, Account seller);
    boolean existsByToolName(String toolName);
}
