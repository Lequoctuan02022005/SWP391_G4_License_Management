// src/main/java/swp391/fa25/lms/controller/cart/CartController.java

package swp391.fa25.lms.controller.cart;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.service.CartService;

import java.util.Map;

@Controller
@RequestMapping("/cart")
public class CartController {

    @Autowired private CartService cartService;

    @PostMapping("/add")
    @ResponseBody
    public ResponseEntity<?> addToCart(
            @RequestParam Long toolId,
            @RequestParam Long licenseId,
            @RequestParam(defaultValue = "1") int quantity,
            HttpServletRequest request) {

        Account account = (Account) request.getSession().getAttribute("loggedInAccount");
        if (account == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("message", "Vui lòng đăng nhập để thêm vào giỏ hàng"));
        }

        try {
            int count = cartService.addToCart(account, toolId, licenseId, quantity);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã thêm vào giỏ hàng!",
                    "count", count
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/update")
    @ResponseBody
    public ResponseEntity<?> updateQuantity(
            @RequestParam Long cartItemId,
            @RequestParam int quantity,
            HttpServletRequest request) {

        Account account = (Account) request.getSession().getAttribute("loggedInAccount");
        if (account == null) return ResponseEntity.status(401).build();

        try {
            cartService.updateQuantity(account, cartItemId, quantity);
            int count = cartService.getCartItemCount(account);
            return ResponseEntity.ok(Map.of("success", true, "count", count));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/remove")
    @ResponseBody
    public ResponseEntity<?> removeItem(
            @RequestParam Long cartItemId,
            HttpServletRequest request) {

        Account account = (Account) request.getSession().getAttribute("loggedInAccount");
        if (account == null) return ResponseEntity.status(401).build();

        cartService.removeItem(account, cartItemId);
        int count = cartService.getCartItemCount(account);
        return ResponseEntity.ok(Map.of("success", true, "count", count));
    }

    @GetMapping("/view")
    public String viewCart(Model model, HttpServletRequest request) {
        Account account = (Account) request.getSession().getAttribute("loggedInAccount");
        if (account == null) return "redirect:/login";

        var cart = cartService.getOrCreateCart(account);
        model.addAttribute("cart", cart);
        model.addAttribute("items", cart.getItems() != null ? cart.getItems() : java.util.Collections.emptyList());
        return "cart/view";
    }

    @GetMapping("/count")
    @ResponseBody
    public ResponseEntity<Integer> getCartCount(HttpServletRequest request) {
        Account account = (Account) request.getSession().getAttribute("loggedInAccount");
        if (account == null) return ResponseEntity.ok(0);
        return ResponseEntity.ok(cartService.getCartItemCount(account));
    }
}