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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

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
        if (!userService.isCustomer(userId)) {
            return "redirect:/";
        }

        List<CartItem> cartItems = cartService.getCartItems(userId);
        if (cartItems.isEmpty()) {
            return "redirect:/cart";
        }

        List<Address> addresses = addressRepo.findByUser_Id(userId);
        List<Country> countries = countryRepo.findAll();

        model.addAttribute("cartItems", cartItems);
        model.addAttribute("addresses", addresses);
        model.addAttribute("countries", countries);
        model.addAttribute("orderRequest", new OrderRequest());

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
}
