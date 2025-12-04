package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import swp391.fa25.lms.model.Category;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    //contextdb
    @Query("SELECT c FROM Category c WHERE " +
            "c.categoryName LIKE %?1% OR " +
            "c.description LIKE %?1%")
    List<Category> search(String keyword);
    List<Category> findByStatus(Category.Status status);
}