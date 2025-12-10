package swp391.fa25.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import swp391.fa25.lms.model.Cart;
import swp391.fa25.lms.model.CartItem;
import swp391.fa25.lms.model.License;
import swp391.fa25.lms.model.Tool;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartAndToolAndLicense(Cart cart, Tool tool, License license);

    @Query("SELECT COALESCE(SUM(ci.quantity), 0) FROM CartItem ci WHERE ci.cart = :cart")
    Integer sumQuantityByCart(@Param("cart") Cart cart);

    // Xóa hết item của 1 cart
    @Transactional
    @Modifying
    void deleteByCart(Cart cart);
}