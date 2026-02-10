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

        List<OrderTrack> tracks = orderTrackRepo.findByOrder_IdOrderByUpdatedTimeAsc(orderId);

        BigDecimal totalProductDiscount = order.getOrderDetails().stream()
                .map(detail -> {
                    if (detail.getOriginalUnitPrice() != null) {
                        return detail.getOriginalUnitPrice()
                                .subtract(detail.getUnitPrice())
                                .multiply(BigDecimal.valueOf(detail.getQuantity()));
                    }
                    return BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("order", order);
        model.addAttribute("tracks", tracks);
        model.addAttribute("totalProductDiscount", totalProductDiscount);
        return "orders/order_detail";
    }
}