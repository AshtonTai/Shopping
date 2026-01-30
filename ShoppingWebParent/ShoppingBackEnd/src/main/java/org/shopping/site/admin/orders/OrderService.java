package org.shopping.site.admin.orders;

import jakarta.transaction.Transactional;
import org.shopping.entity.*;
import org.shopping.entity.product.Product;
import org.shopping.site.admin.address.AddressRepository;
import org.shopping.site.admin.cartitem.CartItemRepository;
import org.shopping.site.admin.product.ProductRepository;
import org.shopping.site.admin.shippingrate.ShippingRateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
public class OrderService {

    @Autowired
    private OrderRepository orderRepo;
    @Autowired private OrderDetailRepository orderDetailRepo;
    @Autowired private CartItemRepository cartItemRepo;
    @Autowired private ProductRepository productRepo;
    @Autowired private AddressRepository addressRepo;
    @Autowired private ShippingRateRepository shippingRateRepo;

    public Order findById(Integer id) {
        return orderRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + id));
    }

    public Order createOrder(Integer customerId, Integer addressId, String paymentMethod, List<CartItem> cartItems) {
        Address address = addressRepo.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        // Calculate subtotal
        BigDecimal subtotal = cartItems.stream()
                .map(item -> {
                    float price = item.getProduct().getPrice();
                    int quantity = item.getQuantity();
                    return BigDecimal.valueOf(price).multiply(BigDecimal.valueOf(quantity));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Get shipping cost
        ShippingRate rate = shippingRateRepo.findByCountry_Id(address.getCountry().getId())
                .stream().findFirst()
                .orElseThrow(() -> new RuntimeException("No shipping rate available"));

        BigDecimal shippingCost = rate.getCost();
        BigDecimal total = subtotal.add(shippingCost);

        // Create order
        Order order = new Order();
        order.setCustomer(new User(customerId));
        order.setShippingAddress(address);
        order.setPaymentMethod(paymentMethod);
        order.setTotalAmount(total);
        order.setShippingCost(shippingCost);
        order.setStatus("PENDING");
        order.setOrderNumber(generateOrderNumber());

        Order savedOrder = orderRepo.save(order);

        // Create order details & deduct stock
        for (CartItem item : cartItems) {
            Product product = item.getProduct();
            float price = product.getPrice();
            int quantity = item.getQuantity();

            OrderDetail detail = new OrderDetail();
            detail.setOrder(savedOrder);
            detail.setProduct(product);
            detail.setQuantity(quantity);
            detail.setUnitPrice(BigDecimal.valueOf(price)); // ✅ Convert float → BigDecimal
            detail.setSubtotal(BigDecimal.valueOf(price).multiply(BigDecimal.valueOf(quantity)));
            orderDetailRepo.save(detail);

            // Deduct stock
            product.setInStock(product.getInStock() - quantity);
            productRepo.save(product);
        }

        // Clear cart
        cartItemRepo.deleteByCustomer_Id(customerId);

        return savedOrder;
    }

    private String generateOrderNumber() {
        return "ORD-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" +
                String.format("%03d", (int)(Math.random() * 1000));
    }
}
