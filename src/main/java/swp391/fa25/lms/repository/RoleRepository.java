package swp391.fa25.lms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp391.fa25.lms.model.Role;
import swp391.fa25.lms.model.Role.RoleName;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByRoleName(RoleName roleName);
    boolean existsByRoleName(RoleName roleName);

    //admin
    Page<Role> findByRoleName(Role.RoleName roleName, Pageable pageable);
    Page<Role> findByNoteContainingIgnoreCase(String note, Pageable pageable);

    @Query("""
        SELECT r FROM Role r 
        WHERE LOWER(r.note) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(r.roleName) LIKE LOWER(CONCAT('%', :keyword, '%'))
    """)
    Page<Role> searchAll(@Param("keyword") String keyword, Pageable pageable);
}
