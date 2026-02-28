package org.shopping.site.admin.dashboard;

import org.shopping.entity.Order;
import org.shopping.site.admin.BaseController;
import org.shopping.site.admin.orders.OrderRepository;
import org.shopping.site.admin.orders.OrderService;
import org.shopping.site.admin.orders.OrderTrackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
    @Autowired private OrderService orderService;
    @Autowired private OrderTrackRepository orderTrackRepo;

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
    public String viewOrderDetail(@PathVariable Integer orderId, Model model, Authentication auth) {
        Order order = orderService.findById(orderId);

        // Calculate breakdown values
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

        model.addAttribute("originalSubtotal", originalSubtotal);
        model.addAttribute("productDiscount", productDiscount);
        model.addAttribute("voucherDiscount", voucherDiscount);
        model.addAttribute("shippingCost", shippingCost);
        model.addAttribute("isFreeShipping", isFreeShipping);
        model.addAttribute("grandTotal", grandTotal);

        return "admin/order_detail";
    }
}