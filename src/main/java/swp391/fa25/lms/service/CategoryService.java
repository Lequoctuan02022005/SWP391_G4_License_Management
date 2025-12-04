package swp391.fa25.lms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.Category;
import swp391.fa25.lms.repository.CategoryRepository;

import java.util.List;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository repo;

    public List<Category> getAll() {
        return repo.findAll();
    public List<Category> getAllCategories() {
        return repo.findByStatus(Category.Status.ACTIVE);
    }

    public Category getById(Long id) {
        return repo.findById(id).orElse(null);
    }

    public Category create(Category c) {
        c.setStatus(Category.Status.ACTIVE);
        return repo.save(c);
    }

    public Category update(Long id, Category c) {
        Category old = getById(id);
        if (old == null) return null;

        old.setCategoryName(c.getCategoryName());
        old.setDescription(c.getDescription());
        old.setIcon(c.getIcon());
        old.setStatus(c.getStatus());

        return repo.save(old);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }

    public List<Category> search(String keyword) {
        return repo.search(keyword);
    }
}
