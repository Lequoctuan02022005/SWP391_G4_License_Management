// src/main/java/swp391/fa25/lms/service/CartService.java

package swp391.fa25.lms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.fa25.lms.model.*;
import swp391.fa25.lms.repository.*;

import java.util.ArrayList;
import java.util.Optional;

@Service
@Transactional
public class CartService {

    @Autowired private CartRepository cartRepository;
    @Autowired private CartItemRepository cartItemRepository;
    @Autowired private ToolRepository toolRepository;
    @Autowired private LicenseToolRepository licenseRepository;

    public Cart getOrCreateCart(Account account) {
        return cartRepository.findByAccount(account)
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setAccount(account);
                    cart.setStatus(Cart.CartStatus.ACTIVE);
                    cart.setItems(new ArrayList<>());
                    return cartRepository.save(cart);
                });
    }

    public int addToCart(Account account, Long toolId, Long licenseId, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Số lượng phải lớn hơn 0");

        Cart cart = getOrCreateCart(account);

        Tool tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại"));

        License license = licenseRepository.findById(licenseId)
                .orElseThrow(() -> new IllegalArgumentException("License không tồn tại"));

        if (tool.getAvailableQuantity() == null || tool.getAvailableQuantity() <= 0) {
            throw new IllegalArgumentException("Sản phẩm đã hết hàng");
        }

        // Tìm item đã tồn tại chưa
        Optional<CartItem> existingOpt = cartItemRepository.findByCartAndToolAndLicense(cart, tool, license);

        if (existingOpt.isPresent()) {
            // Cập nhật số lượng
            CartItem item = existingOpt.get();
            item.setQuantity(item.getQuantity() + quantity);
            // @PreUpdate sẽ tự tính lại totalPrice nhờ phương thức recalcTotal()
            cartItemRepository.saveAndFlush(item); // QUAN TRỌNG: PHẢI SAVE!
        } else {
            // Tạo mới item
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setTool(tool);
            newItem.setLicense(license);
            newItem.setQuantity(quantity);
            newItem.setUnitPrice(license.getPrice() != null ? license.getPrice() : 0.0);
            // totalPrice sẽ được tính tự động trong @PrePersist
            cart.getItems().add(newItem); //  thêm vào collection
            cartItemRepository.saveAndFlush(newItem); // QUAN TRỌNG: PHẢI SAVE!
        }

        return getCartItemCount(account);
    }

    public void updateQuantity(Account account, Long cartItemId, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Số lượng phải lớn hơn 0");

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm trong giỏ hàng"));

        if (!item.getCart().getAccount().equals(account)) {
            throw new SecurityException("Không được sửa giỏ hàng của người khác");
        }

        item.setQuantity(quantity);
        cartItemRepository.save(item); // Sẽ tự gọi @PreUpdate → tính lại totalPrice
    }

    public void removeItem(Account account, Long cartItemId) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));

        if (!item.getCart().getAccount().equals(account)) {
            throw new SecurityException("Không được xóa sản phẩm của người khác");
        }

        cartItemRepository.delete(item);
    }

    public int getCartItemCount(Account account) {
        return cartRepository.findByAccount(account)
                .map(cart -> cartItemRepository.sumQuantityByCart(cart))
                .orElse(0);
    }
}