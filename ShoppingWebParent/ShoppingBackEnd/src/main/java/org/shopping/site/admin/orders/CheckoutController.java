package org.shopping.site.admin.orders;

import org.shopping.entity.*;
import org.shopping.site.admin.BaseController;
import org.shopping.site.admin.address.AddressRepository;
import org.shopping.site.admin.cartitem.CartService;
import org.shopping.site.admin.country.CountryRepository;
import org.shopping.site.admin.shippingrate.ShippingRateRepository;
import org.shopping.site.admin.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Controller
public class CheckoutController extends BaseController {

    @Autowired
    private AddressRepository addressRepo;
    @Autowired
    private CountryRepository countryRepo;
    @Autowired
    private ShippingRateRepository shippingRateRepo;
    @Autowired
    private CartService cartService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private UserService userService;

    @GetMapping("/checkout")
    public String showCheckout(Model model, Authentication auth) {
        Integer userId = getCurrentUserId(auth);

        if (!userService.isCustomer(userId)) {
            return "redirect:/";
        }

        List<CartItem> cartItems = cartService.getCartItems(userId);
        if (cartItems == null || cartItems.isEmpty()) {
            return "redirect:/cart/cart";
        }

        int totalQuantity = cartItems.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();

        List<Address> addresses = addressRepo.findByUser_Id(userId);
        List<Country> countries = countryRepo.findAll();

        BigDecimal cartTotal = cartItems.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getProduct() != null)
                .map(item -> {
                    BigDecimal price = BigDecimal.valueOf(item.getProduct().getPrice());
                    return price.multiply(BigDecimal.valueOf(item.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal shippingFee = BigDecimal.ZERO;
        Address selectedAddress = null;

        if (!addresses.isEmpty()) {
            Address addr = addresses.get(0);
            ShippingRate rateObj = shippingRateRepo.findByCountryAndState(
                    addr.getCountry(),
                    addr.getState()
            );
            if (rateObj != null) {
                shippingFee = rateObj.getRate();
            }
        }

        BigDecimal grandTotal = cartTotal.add(shippingFee);

        model.addAttribute("cartItems", cartItems);
        model.addAttribute("addresses", addresses);
        model.addAttribute("countries", countries);
        model.addAttribute("orderRequest", new OrderRequest());
        model.addAttribute("totalQuantity", totalQuantity);
        model.addAttribute("cartTotal", cartTotal);
        model.addAttribute("shippingFee", shippingFee);
        model.addAttribute("grandTotal", grandTotal);
        if (selectedAddress != null) {
            model.addAttribute("defaultAddressId", selectedAddress.getId());
        }

        return "checkout";
    }

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

            return "redirect:/orders/success/" + order.getId();
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

    @GetMapping("/api/shipping-fee")
    @ResponseBody
    public ResponseEntity<BigDecimal> getShippingFee(
            @RequestParam(required = false) Integer addressId,
            Authentication auth) {

        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(BigDecimal.ZERO);
        }

        Integer userId = getCurrentUserId(auth);
        if (userId == null || !userService.isCustomer(userId)) {
            return ResponseEntity.badRequest().body(BigDecimal.ZERO);
        }

        if (addressId == null || addressId <= 0) {
            return ResponseEntity.ok(BigDecimal.ZERO);
        }

        try {
            Address address = addressRepo.findById(addressId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid address"));

            if (!address.getUser().getId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(BigDecimal.ZERO);
            }

            ShippingRate rate = shippingRateRepo.findByCountryAndState(
                    address.getCountry(),
                    address.getState()
            );

            return ResponseEntity.ok(rate != null ? rate.getRate() : BigDecimal.ZERO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(BigDecimal.ZERO);
        }
    }
}