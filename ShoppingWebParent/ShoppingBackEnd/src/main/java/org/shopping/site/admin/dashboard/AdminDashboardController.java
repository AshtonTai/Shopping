package org.shopping.site.admin.dashboard;

import org.shopping.entity.Order;
import org.shopping.site.admin.BaseController;
import org.shopping.site.admin.orders.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasAuthority('Admin')")
public class AdminDashboardController extends BaseController {

    @Autowired private OrderRepository orderRepo;

    @GetMapping
    public String dashboard(@RequestParam(required = false) String date, Model model) {
        LocalDate selectedDate;

        if (date != null && !date.trim().isEmpty()) {
            selectedDate = LocalDate.parse(date);
        } else {
            selectedDate = LocalDate.now();
        }

        List<Order> todaysOrders = orderRepo.findByOrderDate(selectedDate);

        // Calculate stats
        BigDecimal totalRevenue = todaysOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pendingCount = todaysOrders.stream()
                .filter(order -> "PENDING".equals(order.getStatus()))
                .count();

        long deliveredCount = todaysOrders.stream()
                .filter(order -> "DELIVERED".equals(order.getStatus()))
                .count();

        model.addAttribute("todaysOrders", todaysOrders);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("yesterday", selectedDate.minusDays(1));
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("deliveredCount", deliveredCount);

        return "admin/dashboard";
    }

    @GetMapping("/order/{orderId}")
    public String viewOrder(@PathVariable Integer orderId, Model model) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        model.addAttribute("order", order);
        return "admin/order_detail";
    }
}