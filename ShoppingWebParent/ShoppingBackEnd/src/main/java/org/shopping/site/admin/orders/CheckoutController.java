package org.shopping.site.admin.orders;

import org.shopping.entity.*;
import org.shopping.entity.product.Product;
import org.shopping.site.admin.BaseController;
import org.shopping.site.admin.address.AddressRepository;
import org.shopping.site.admin.cartitem.CartItemRepository;
import org.shopping.site.admin.cartitem.CartService;
import org.shopping.site.admin.country.CountryRepository;
import org.shopping.site.admin.price.PriceService;
import org.shopping.site.admin.product.ProductRepository;
import org.shopping.site.admin.shippingrate.ShippingRateRepository;
import org.shopping.site.admin.user.UserService;
import org.shopping.site.admin.voucher.VoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Controller
public class CheckoutController extends BaseController {

    @Autowired
    private AddressRepository addressRepo;
    @Autowired
    private OrderRepository orderRepo;
    @Autowired
    private OrderDetailRepository orderDetailRepo;
    @Autowired
    private ProductRepository productRepo;
    @Autowired
    private CountryRepository countryRepo;
    @Autowired
    private CartItemRepository cartItemRepo;
    @Autowired
    private ShippingRateRepository shippingRateRepo;
    @Autowired
    private CartService cartService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private UserService userService;
    @Autowired
    private PriceService priceService;
    @Autowired
    private VoucherService voucherService;

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

        BigDecimal cartTotal = BigDecimal.ZERO;
        BigDecimal totalProductDiscount = BigDecimal.ZERO;

        for (CartItem item : cartItems) {
            if (item.getProduct() == null) continue;

            Product p = item.getProduct();
            double originalPrice = p.getPrice();
            double finalPrice = (p.getDiscountPercent() > 0)
                    ? p.getDiscountPrice()
                    : p.getPrice();

            BigDecimal itemTotal = BigDecimal.valueOf(finalPrice).multiply(BigDecimal.valueOf(item.getQuantity()));
            cartTotal = cartTotal.add(itemTotal);

            // Calculate how much was saved on this item due to product discount
            BigDecimal originalItemTotal = BigDecimal.valueOf(originalPrice).multiply(BigDecimal.valueOf(item.getQuantity()));
            BigDecimal itemDiscount = originalItemTotal.subtract(itemTotal);
            totalProductDiscount = totalProductDiscount.add(itemDiscount);
        }

        BigDecimal shippingFee = BigDecimal.ZERO;
        if (!addresses.isEmpty()) {
            Address addr = addresses.get(0);
            ShippingRate rateObj = shippingRateRepo.findByCountryAndState(addr.getCountry(), addr.getState());
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
        model.addAttribute("cartTotal", cartTotal); // Final price after product discounts
        model.addAttribute("totalProductDiscount", totalProductDiscount); // 👈 New!
        model.addAttribute("shippingFee", shippingFee);
        model.addAttribute("grandTotal", grandTotal);
        if (!addresses.isEmpty()) {
            model.addAttribute("defaultAddressId", addresses.get(0).getId());
        }

        return "checkout";
    }

    @PostMapping("/checkout")
    public String processOrder(@ModelAttribute OrderRequest request,
                               @RequestParam(required = false) String voucherCode,
                               Authentication auth,
                               RedirectAttributes redirectAttributes) {
        Integer userId = getCurrentUserId(auth);
        if (!userService.isCustomer(userId)) {
            return "redirect:/";
        }

        try {
            Address address = addressRepo.findById(request.getAddressId())
                    .orElseThrow(() -> new RuntimeException("Address not found"));

            List<CartItem> cartItems = cartService.getCartItems(userId);
            if (cartItems.isEmpty()) {
                throw new RuntimeException("Cart is empty");
            }

            Voucher appliedVoucher = null;
            if (voucherCode != null && !voucherCode.trim().isEmpty()) {
                appliedVoucher = voucherService.validateVoucher(voucherCode.trim(), userId);
            }

            Order savedOrder = orderService.createOrder(userId, address.getId(), request.getPaymentMethod(), cartItems, appliedVoucher);

            return "redirect:/orders/success/" + savedOrder.getId();
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

    @PostMapping("/api/validate-voucher")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> validateVoucher(
            @RequestBody Map<String, Object> request,
            Authentication auth) {

        try {
            String code = (String) request.get("code");
            Integer addressId = ((Number) request.get("addressId")).intValue();
            Integer userId = getCurrentUserId(auth);

            if (code == null || code.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("valid", false, "message", "Voucher code is required"));
            }

            Voucher voucher = voucherService.validateVoucher(code.trim(), userId);

            List<CartItem> cartItems = cartService.getCartItems(userId);

            BigDecimal cartTotal = priceService.calculateCartSubtotal(cartItems);

            BigDecimal discountAmount = voucherService.calculateDiscount(voucher, cartTotal);
            BigDecimal discountedSubtotal = cartTotal.subtract(discountAmount);

            Address address = addressRepo.findById(addressId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid address"));

            BigDecimal shippingCost = BigDecimal.ZERO;
            if (voucher.getType() != VoucherType.FREE_SHIPPING) {
                ShippingRate rate = shippingRateRepo.findByCountryAndState(
                        address.getCountry(), address.getState()
                );
                shippingCost = rate != null ? rate.getRate() : BigDecimal.ZERO;
            }

            BigDecimal grandTotal = discountedSubtotal.add(shippingCost);

            Map<String, Object> response = new HashMap<>();
            response.put("valid", true);
            response.put("cartTotal", discountedSubtotal.doubleValue());
            response.put("discountAmount", discountAmount.doubleValue());
            response.put("shippingCost", shippingCost.doubleValue());
            response.put("grandTotal", grandTotal.doubleValue());
            response.put("message", "Voucher applied successfully!");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("valid", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}