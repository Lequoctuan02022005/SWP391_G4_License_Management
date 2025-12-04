package swp391.fa25.lms.repository.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.Role;

import java.util.List;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    // 1. Search theo Enum EXACT
    Page<Role> findByRoleName(Role.RoleName roleName, Pageable pageable);

    // 2. Search theo Note (String)
    Page<Role> findByNoteContainingIgnoreCase(String note, Pageable pageable);

    // 3. Search cả Enum + Note bằng @Query
    @Query("""
        SELECT r FROM Role r 
        WHERE LOWER(r.note) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(r.roleName) LIKE LOWER(CONCAT('%', :keyword, '%'))
    """)
    Page<Role> searchAll(@Param("keyword") String keyword, Pageable pageable);
}
