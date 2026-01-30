package org.shopping.site.admin.orders;

import org.shopping.entity.Order;
import org.shopping.site.admin.BaseController;
import org.shopping.site.admin.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class OrderController extends BaseController {

    @Autowired
    private OrderRepository orderRepo;
    @Autowired private OrderDetailRepository orderDetailRepo;
    @Autowired private OrderTrackRepository orderTrackRepo;
    @Autowired private UserService userService;

    // Order success page
    @GetMapping("/order/success/{orderId}")
    public String orderSuccess(@PathVariable Integer orderId,
                               Model model,
                               Authentication auth) {
        Integer userId = getCurrentUserId(auth);
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Only allow customer or staff to view
        if (!userService.isCustomer(userId) &&
                !(userService.isCustomer(userId) ||
                        userService.isShipper(userId) ||
                        userService.isAdminOrEditor(userId))) {
            return "redirect:/";
        }

        if (userService.isCustomer(userId) && !order.getCustomer().getId().equals(userId)) {
            return "redirect:/";
        }

        model.addAttribute("order", order);
        model.addAttribute("orderDetails", order.getOrderDetails());
        return "order-success";
    }

    // Customer's order history
    @GetMapping("/orders")
    public String myOrders(Model model, Authentication auth) {
        Integer userId = getCurrentUserId(auth);
        if (!userService.isCustomer(userId)) {
            return "redirect:/";
        }
        model.addAttribute("orders", orderRepo.findByCustomer_Id(userId));
        return "orders/orders";
    }

    // Admin/Shipper/Editor: view all orders
    @GetMapping("/admin/orders")
    public String allOrders(Model model, Authentication auth) {
        Integer userId = getCurrentUserId(auth);
        if (!(userService.isCustomer(userId) ||
                userService.isShipper(userId) ||
                userService.isAdminOrEditor(userId))) {
            return "redirect:/";
        }
        model.addAttribute("orders", orderRepo.findAll());
        return "admin-orders";
    }

    // Order tracking page
    @GetMapping("/order/track/{orderId}")
    public String trackOrder(@PathVariable Integer orderId,
                             Model model,
                             Authentication auth) {
        Integer userId = getCurrentUserId(auth);
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Allow customer or staff
        if (!userService.isCustomer(userId) &&
                !(userService.isCustomer(userId) ||
                        userService.isShipper(userId) ||
                        userService.isAdminOrEditor(userId))) {
            return "redirect:/";
        }
        if (userService.isCustomer(userId) && !order.getCustomer().getId().equals(userId)) {
            return "redirect:/";
        }

        model.addAttribute("order", order);
        model.addAttribute("tracks", orderTrackRepo.findByOrder_IdOrderByUpdatedTimeAsc(orderId));
        return "order-track";
    }
}
