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

    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private CartItemRepository cartItemRepository;
    @Autowired
    private ToolRepository toolRepository;
    @Autowired
    private LicenseToolRepository licenseRepository;

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
        if (quantity <= 0)
            throw new IllegalArgumentException("Số lượng phải lớn hơn 0");

        Cart cart = getOrCreateCart(account);

        Tool tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại"));

        License license = licenseRepository.findById(licenseId)
                .orElseThrow(() -> new IllegalArgumentException("License không tồn tại"));

        if (tool.getAvailableQuantity() == null || tool.getAvailableQuantity() <= 0) {
            throw new IllegalArgumentException("Sản phẩm đã hết hàng");
        }

        Optional<CartItem> existingOpt = cartItemRepository.findByCartAndToolAndLicense(cart, tool, license);

        if (existingOpt.isPresent()) {
            CartItem item = existingOpt.get();
            item.setQuantity(item.getQuantity() + quantity);
            cartItemRepository.saveAndFlush(item);
        } else {
            // Tạo mới item
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setTool(tool);
            newItem.setLicense(license);
            newItem.setQuantity(quantity);
            newItem.setUnitPrice(license.getPrice() != null ? license.getPrice() : 0.0);
            cart.getItems().add(newItem);
            cartItemRepository.saveAndFlush(newItem);
        }

        return getCartItemCount(account);
    }

    public void updateQuantity(Account account, Long cartItemId, int quantity) {

        if (quantity < 1) {
            throw new IllegalArgumentException("Số lượng phải lớn hơn 0");
        }

        Cart cart = cartRepository.findByAccount(account)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giỏ hàng"));

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong giỏ hàng"));

        // ❗ Bảo mật: item phải thuộc cart của user
        if (!item.getCart().getCartId().equals(cart.getCartId())) {
            throw new RuntimeException("Không có quyền cập nhật sản phẩm này");
        }

        Tool tool = item.getTool();

        Integer available = tool.getAvailableQuantity();
        if (available == null) {
            throw new IllegalStateException("Sản phẩm chưa thiết lập số lượng tồn kho");
        }

        // ✅ VALIDATE TỒN KHO
        if (quantity > available) {
            throw new IllegalArgumentException(
                    "Trong kho chỉ còn " + available + " sản phẩm, không thể thêm quá số lượng trong kho"
            );
        }

        // OK → update
        item.setQuantity(quantity); // totalPrice auto recalc
        cartItemRepository.save(item);
    }


    public void removeItem(Account account, Long cartItemId) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));

        if (!item.getCart().getAccount().getAccountId().equals(account.getAccountId())) {
            throw new SecurityException("Không được xóa sản phẩm của người khác");
        }

        cartItemRepository.delete(item);
    }

    public int getCartItemCount(Account account) {
        return cartRepository.findByAccount(account)
                .map(cart -> cartItemRepository.sumQuantityByCart(cart))
                .orElse(0);
    }

    public void clearCart(Account account) {
        Cart cart = cartRepository.findByAccountWithItems(account).orElse(null);
        if (cart != null) {
            var items = cart.getItems();
            if (items != null && !items.isEmpty()) {
                cartItemRepository.deleteAll(items);
                cartItemRepository.flush();

                items.clear();
            } else {
                System.out.println("Cart is already empty");
            }
        }
    }

    public Cart getCartWithItems(Account account) {
        return cartRepository.findByAccountWithItems(account)
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setAccount(account);
                    cart.setStatus(Cart.CartStatus.ACTIVE);
                    cart.setItems(new ArrayList<>());
                    return cartRepository.save(cart);
                });
    }
}