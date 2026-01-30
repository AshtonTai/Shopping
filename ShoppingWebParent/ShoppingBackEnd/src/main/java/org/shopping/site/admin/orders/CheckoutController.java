package org.shopping.site.admin.orders;

import org.shopping.entity.Address;
import org.shopping.entity.CartItem;
import org.shopping.entity.Country;
import org.shopping.entity.Order;
import org.shopping.site.admin.BaseController;
import org.shopping.site.admin.address.AddressRepository;
import org.shopping.site.admin.cartitem.CartService;
import org.shopping.site.admin.country.CountryRepository;
import org.shopping.site.admin.shippingrate.ShippingRateRepository;
import org.shopping.site.admin.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Controller
public class CheckoutController extends BaseController {

    @Autowired
    private AddressRepository addressRepo;
    @Autowired private CountryRepository countryRepo;
    @Autowired private ShippingRateRepository shippingRateRepo;
    @Autowired private CartService cartService;
    @Autowired private OrderService orderService;
    @Autowired private UserService userService;

    // Show checkout page
    @GetMapping("/checkout")
    public String showCheckout(Model model, Authentication auth) {
        Integer userId = getCurrentUserId(auth);

        // Early redirect if not a customer
        if (!userService.isCustomer(userId)) {
            return "redirect:/";
        }

        // Get cart items (handle null safely)
        List<CartItem> cartItems = cartService.getCartItems(userId);
        if (cartItems == null) {
            cartItems = List.of();
        }

        // Redirect if cart is empty
        if (cartItems.isEmpty()) {
            return "redirect:cart/cart";
        }

        // Calculate total quantity
        int totalQuantity = cartItems.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();

        // Fetch additional data
        List<Address> addresses = addressRepo.findByUser_Id(userId);
        List<Country> countries = countryRepo.findAll();

        BigDecimal total = cartItems.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getProduct() != null)
                .map(item -> {
                    BigDecimal price = BigDecimal.valueOf(item.getProduct().getPrice());
                    return price.multiply(BigDecimal.valueOf(item.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Add to model
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("addresses", addresses);
        model.addAttribute("countries", countries);
        model.addAttribute("orderRequest", new OrderRequest());
        model.addAttribute("totalQuantity", totalQuantity);
        model.addAttribute("cartTotal", total);

        return "checkout";
    }
    // Process order
    @PostMapping("/checkout")
    public String processOrder(@ModelAttribute OrderRequest request,
                               Authentication auth,
                               RedirectAttributes redirectAttributes) {
        Integer userId = getCurrentUserId(auth);
        if (!userService.isCustomer(userId)) {
            return "redirect:/";
        }

        try {
            List<CartItem> cartItems = cartService.getCartItems(userId);
            if (cartItems.isEmpty()) {
                throw new RuntimeException("Cart is empty");
            }

            Order order = orderService.createOrder(
                    userId,
                    request.getAddressId(),
                    request.getPaymentMethod(),
                    cartItems
            );

            return "redirect:/order/success/" + order.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/checkout";
        }
    }
    @GetMapping("/orders/success/{orderId}")
    public String showOrderSuccess(@PathVariable Integer orderId, Model model) {
        Order order = orderService.findById(orderId);
        if (order == null) {
            return "redirect:/orders?error=Order+not+found";
        }
        model.addAttribute("order", order);
        return "orders/success";
    }
}
