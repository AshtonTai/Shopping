package org.shopping.site.admin.orders;

import org.shopping.entity.Order;
import org.shopping.entity.OrderTrack;
import org.shopping.site.admin.BaseController;
import org.shopping.site.admin.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class OrderController extends BaseController {

    @Autowired
    private OrderRepository orderRepo;
    @Autowired private OrderDetailRepository orderDetailRepo;
    @Autowired private OrderTrackRepository orderTrackRepo;
    @Autowired private UserService userService;
    @Autowired private OrderService orderService;

    @GetMapping("/order/success/{orderId}")
    public String orderSuccess(@PathVariable Integer orderId,
                               Model model,
                               Authentication auth) {
        Integer userId = getCurrentUserId(auth);
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Security check
        if (userService.isCustomer(userId)) {
            if (!order.getCustomer().getId().equals(userId)) {
                return "redirect:/";
            }
        } else if (!(userService.isShipper(userId) || userService.isAdminOrEditor(userId))) {
            return "redirect:/";
        }

        model.addAttribute("order", order);
        model.addAttribute("orderDetails", order.getOrderDetails());
        return "order-success";
    }

    @GetMapping("/orders")
    public String myOrders(Model model, Authentication auth) {
        Integer userId = getCurrentUserId(auth);
        if (!userService.isCustomer(userId)) {
            return "redirect:/";
        }
        model.addAttribute("listOrders", orderRepo.findByCustomer_Id(userId));
        return "orders/orders";
    }

    @GetMapping("/admin/orders")
    public String allOrders(Model model, Authentication auth) {
        Integer userId = getCurrentUserId(auth);
        // Only shippers and admins/editors can see all orders
        if (!(userService.isShipper(userId) || userService.isAdminOrEditor(userId))) {
            return "redirect:/";
        }
        model.addAttribute("orders", orderRepo.findAll());
        return "admin-orders";
    }

    @GetMapping("/order/track/{orderId}")
    public String trackOrder(@PathVariable Integer orderId,
                             Model model,
                             Authentication auth) {
        Integer userId = getCurrentUserId(auth);
        Order order = orderRepo.findByIdWithVoucher(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Security check
        if (userService.isCustomer(userId)) {
            if (!order.getCustomer().getId().equals(userId)) {
                return "redirect:/";
            }
        } else if (!(userService.isShipper(userId) || userService.isAdminOrEditor(userId))) {
            return "redirect:/";
        }

        model.addAttribute("order", order);
        model.addAttribute("tracks", orderTrackRepo.findByOrder_IdOrderByUpdatedTimeAsc(orderId));
        return "order-track";
    }

    @GetMapping("/orders/detail/{orderId}")
    public String viewOrderDetail(@PathVariable Integer orderId, Model model, Authentication auth) {
        Integer userId = getCurrentUserId(auth);
        Order order = orderService.findById(orderId);

        if (!order.getCustomer().getId().equals(userId)) {
            return "redirect:/orders";
        }

        // Calculate all breakdown values in Java (safe, testable, no template logic)
        BigDecimal originalSubtotal = order.getOrderDetails().stream()
                .map(d -> d.getOriginalUnitPrice() != null
                        ? d.getOriginalUnitPrice().multiply(BigDecimal.valueOf(d.getQuantity()))
                        : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal productDiscount = order.getOrderDetails().stream()
                .map(d -> d.getOriginalUnitPrice() != null && d.getUnitPrice() != null
                        ? d.getOriginalUnitPrice().subtract(d.getUnitPrice()).multiply(BigDecimal.valueOf(d.getQuantity()))
                        : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal voucherDiscount = order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal shippingCost = order.getShippingCost() != null ? order.getShippingCost() : BigDecimal.ZERO;
        BigDecimal grandTotal = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        boolean isFreeShipping = shippingCost.compareTo(BigDecimal.ZERO) == 0;

        model.addAttribute("order", order);
        model.addAttribute("tracks", orderTrackRepo.findByOrder_IdOrderByUpdatedTimeAsc(orderId));

        // Pass pre-calculated values to template
        model.addAttribute("originalSubtotal", originalSubtotal);
        model.addAttribute("productDiscount", productDiscount);
        model.addAttribute("voucherDiscount", voucherDiscount);
        model.addAttribute("shippingCost", shippingCost);
        model.addAttribute("isFreeShipping", isFreeShipping);
        model.addAttribute("grandTotal", grandTotal);

        return "orders/order_detail";
    }
}