package swp391.fa25.lms.service.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.Role;
import swp391.fa25.lms.repository.admin.RoleRepository;

import java.util.List;

@Service
public class RoleService {

    @Autowired
    private RoleRepository repo;

    public Page<Role> search(String keyword, Pageable pageable) {

        if (keyword == null || keyword.isBlank()) {
            return repo.findAll(pageable);
        }

        // JPQL search cả ENUM + NOTE
        return repo.searchAll(keyword.trim(), pageable);
    }

    public Page<Role> getAll(Pageable pageable) {
        return repo.findAll(pageable);
    }

    public Role getById(Integer id) {
        return repo.findById(id).orElse(null);
    }

    public Role create(Role role) {
        return repo.save(role);
    }

    public Role update(Integer id, Role newRole) {
        Role r = getById(id);
        if (r == null) return null;

        r.setNote(newRole.getNote());
//        r.setRoleName(newRole.getRoleName());
        return repo.save(r);
    }
}
