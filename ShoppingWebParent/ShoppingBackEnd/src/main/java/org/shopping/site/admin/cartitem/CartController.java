package org.shopping.site.admin.cartitem;

import org.shopping.entity.CartItem;
import org.shopping.entity.User;
import org.shopping.site.admin.BaseController;
import org.shopping.site.admin.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class CartController extends BaseController {

    @Autowired private CartService cartService;
    @Autowired private UserService userService;

    // Add item to cart (called from product page)
    @PostMapping("/cart/add")
    public String addToCart(@RequestParam Integer productId,
                            @RequestParam(defaultValue = "1") Integer quantity,
                            Authentication auth,
                            RedirectAttributes redirectAttributes) {
        Integer userId = getCurrentUserId(auth);

        if (!userService.isCustomer(userId)) {
            redirectAttributes.addFlashAttribute("error", "Only customers can add to cart.");
            return "redirect:/products/" + productId;
        }

        try {
            cartService.addToCart(userId, productId, quantity);
            redirectAttributes.addFlashAttribute("success", "Added to cart!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/products/" + productId;
    }

    // View cart
    @GetMapping("/cart")
    public String viewCart(Model model, Authentication auth) {
        Integer userId = getCurrentUserId(auth);
        if (!userService.isCustomer(userId)) {
            return "redirect:/";
        }

        List<CartItem> cartItems = cartService.getCartItems(userId);

        // Calculate total
        BigDecimal total = cartItems.stream()
                .map(item -> {
                    // Convert float price to BigDecimal safely
                    BigDecimal price = BigDecimal.valueOf(item.getProduct().getPrice());
                    return price.multiply(BigDecimal.valueOf(item.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);


        model.addAttribute("cartItems", cartItems);
        model.addAttribute("cartTotal", total);
        return "cart";
    }

    // Remove item from cart
    @PostMapping("/cart/remove")
    public String removeFromCart(@RequestParam Integer itemId,
                                 Authentication auth,
                                 RedirectAttributes redirectAttributes) {
        Integer userId = getCurrentUserId(auth);
        // Optional: verify item belongs to user
        cartService.removeFromCart(itemId);
        redirectAttributes.addFlashAttribute("success", "Item removed.");
        return "redirect:/cart";
    }

    // Clear entire cart
    @PostMapping("/cart/clear")
    public String clearCart(Authentication auth) {
        Integer userId = getCurrentUserId(auth);
        cartService.clearCart(userId);
        return "redirect:/cart";
    }
}