package swp391.fa25.lms.controller.checkout;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.service.CartService;

@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    @Autowired
    private CartService cartService;

    @GetMapping
    public String showCheckout(Model model, HttpServletRequest request) {

        Account account = (Account) request.getSession().getAttribute("loggedInAccount");
        if (account == null) {
            return "redirect:/login";
        }

        var cart = cartService.getCartWithItems(account);
        var items = cart.getItems();

        if (items == null || items.isEmpty()) {
            return "redirect:/cart/view";
        }

        // Tính tổng tiền
        double total = items.stream()
                .mapToDouble(item -> item.getTotalPrice())
                .sum();

        model.addAttribute("account", account);
        model.addAttribute("items", items);
        model.addAttribute("total", total);

        return "checkout/confirm";
    }

    @PostMapping("/complete")
    public String completeCheckout(HttpServletRequest request, RedirectAttributes redirectAttrs) {
        Account account = (Account) request.getSession().getAttribute("loggedInAccount");
        if (account == null) {
            return "redirect:/login";
        }

        try {
            var cart = cartService.getCartWithItems(account);
            var items = cart.getItems();

            if (items == null || items.isEmpty()) {
                redirectAttrs.addFlashAttribute("error", "Giỏ hàng trống, không thể thanh toán");
                return "redirect:/cart/view";
            }
            cartService.clearCart(account);

            redirectAttrs.addFlashAttribute("success", "Thanh toán thành công! Cảm ơn bạn đã mua hàng.");
            return "redirect:/checkout/success";
        } catch (Exception e) {
            System.err.println("Checkout error: " + e.getMessage());
            e.printStackTrace();
            redirectAttrs.addFlashAttribute("error", "Có lỗi xảy ra trong quá trình thanh toán: " + e.getMessage());
            return "redirect:/checkout";
        }
    }

    @GetMapping("/success")
    public String checkoutSuccess(Model model, HttpServletRequest request) {
        Account account = (Account) request.getSession().getAttribute("loggedInAccount");
        if (account == null) {
            return "redirect:/login";
        }

        model.addAttribute("account", account);
        return "checkout/success";
    }
}