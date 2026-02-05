package org.shopping.site.admin.dashboard;

import org.shopping.entity.Order;
import org.shopping.site.admin.BaseController;
import org.shopping.site.admin.orders.OrderService;
import org.shopping.site.admin.orders.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/shipper")
@PreAuthorize("hasAuthority('Shipper')")
public class ShipperController extends BaseController {

    @Autowired private OrderRepository orderRepo;
    @Autowired private OrderService orderService;

    @GetMapping
    public String dashboard(Model model) {
        // Get PENDING orders (need confirmation)
        List<Order> pendingOrders = orderRepo.findByStatus("PENDING");

        // Get orders ready to ship (CONFIRMED status)
        List<Order> readyToShipOrders = orderRepo.findByStatus("CONFIRMED");

        // Get orders in transit (SHIPPED status)
        List<Order> inTransitOrders = orderRepo.findByStatus("SHIPPED");

        // Get orders delivered today
        LocalDate today = LocalDate.now();
        List<Order> deliveredTodayOrders = orderRepo.findByStatusAndOrderDate("DELIVERED", today);

        // Get recent orders (last 10 orders)
        List<Order> recentOrders = orderRepo.findTop10ByOrderByOrderTimeDesc();

        model.addAttribute("pendingOrders", pendingOrders);
        model.addAttribute("readyToShipOrders", readyToShipOrders);
        model.addAttribute("inTransitOrders", inTransitOrders);
        model.addAttribute("deliveredTodayOrders", deliveredTodayOrders);
        model.addAttribute("recentOrders", recentOrders);

        return "shipper/dashboard";
    }

    @PostMapping("/confirm/{orderId}")
    public String confirmOrder(@PathVariable Integer orderId, RedirectAttributes redirectAttributes) {
        try {
            orderService.confirmOrder(orderId);
            redirectAttributes.addFlashAttribute("message", "Order confirmed successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to confirm order: " + e.getMessage());
        }
        return "redirect:/shipper";
    }

    @PostMapping("/ship/{orderId}")
    public String shipOrder(@PathVariable Integer orderId,
                            RedirectAttributes redirectAttributes) {
        try {
            orderService.shipOrder(orderId);
            redirectAttributes.addFlashAttribute("message", "Order marked as shipped!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to ship order: " + e.getMessage());
        }
        return "redirect:/shipper";
    }

    @PostMapping("/deliver/{orderId}")
    public String deliverOrder(@PathVariable Integer orderId,
                               RedirectAttributes redirectAttributes) {
        try {
            orderService.deliverOrder(orderId);
            redirectAttributes.addFlashAttribute("message", "Order marked as delivered!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to deliver order: " + e.getMessage());
        }
        return "redirect:/shipper";
    }

    @GetMapping("/order/{orderId}")
    public String viewOrder(@PathVariable Integer orderId, Model model) {
        Order order = orderService.findById(orderId);
        model.addAttribute("order", order);
        return "shipper/order_detail";
    }
}