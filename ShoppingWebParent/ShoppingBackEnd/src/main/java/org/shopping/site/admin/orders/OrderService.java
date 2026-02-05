package org.shopping.site.admin.orders;

import jakarta.transaction.Transactional;
import org.shopping.entity.*;
import org.shopping.entity.product.Product;
import org.shopping.site.admin.address.AddressRepository;
import org.shopping.site.admin.cartitem.CartItemRepository;
import org.shopping.site.admin.orders.constant.OrderStatus;
import org.shopping.site.admin.product.ProductRepository;
import org.shopping.site.admin.shippingrate.ShippingRateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
public class OrderService {

    @Autowired
    private OrderRepository orderRepo;
    @Autowired private OrderDetailRepository orderDetailRepo;
    @Autowired private OrderTrackRepository orderTrackRepo;
    @Autowired private CartItemRepository cartItemRepo;
    @Autowired private ProductRepository productRepo;
    @Autowired private AddressRepository addressRepo;
    @Autowired private ShippingRateRepository shippingRateRepo;
    @Autowired private OrderEventService orderEventService;

    public Order findById(Integer id) {
        return orderRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + id));
    }

    public Order createOrder(Integer customerId, Integer addressId, String paymentMethod, List<CartItem> cartItems) {
        Address address = addressRepo.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        if (address.getCountry() == null) {
            throw new RuntimeException("Address must have a country");
        }
//        if (address.getState() == null) {
//            throw new RuntimeException("Address must have a state");
//        }

        BigDecimal subtotal = cartItems.stream()
                .map(item -> {
                    float price = item.getProduct().getPrice();
                    int quantity = item.getQuantity();
                    return BigDecimal.valueOf(price).multiply(BigDecimal.valueOf(quantity));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);


        ShippingRate rate = shippingRateRepo.findByCountryAndState(
                address.getCountry(),
                address.getState()
        );

        if (rate == null) {
            throw new RuntimeException("No shipping rate available for " +
                    address.getCountry().getName() + " - " + address.getState().getName());
        }

        BigDecimal shippingCost = rate.getRate();
        BigDecimal total = subtotal.add(shippingCost);

        Order order = new Order();
        order.setCustomer(new User(customerId));
        order.setShippingAddress(address);
        order.setPaymentMethod(paymentMethod);
        order.setTotalAmount(total);
        order.setShippingCost(shippingCost);
        order.setStatus(OrderStatus.PENDING);
        order.setOrderNumber(generateOrderNumber());

        Order savedOrder = orderRepo.save(order);

        for (CartItem item : cartItems) {
            Product product = item.getProduct();
            float price = product.getPrice();
            int quantity = item.getQuantity();

            OrderDetail detail = new OrderDetail();
            detail.setOrder(savedOrder);
            detail.setProduct(product);
            detail.setQuantity(quantity);
            detail.setUnitPrice(BigDecimal.valueOf(price));
            detail.setSubtotal(BigDecimal.valueOf(price).multiply(BigDecimal.valueOf(quantity)));
            orderDetailRepo.save(detail);

            product.setInStock(product.getInStock() - quantity);
            productRepo.save(product);
        }

        cartItemRepo.deleteByCustomer_Id(customerId);

        createOrderTrack(savedOrder, OrderStatus.PENDING);

        OrderEvent event = createOrderEvent(savedOrder, "CREATED");
        orderEventService.broadcastOrderEvent(event);

        return savedOrder;
    }

    public void confirmOrder(Integer orderId) {
        Order order = findById(orderId);
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepo.save(order);
        createOrderTrack(order, OrderStatus.CONFIRMED);

        OrderEvent event = createOrderEvent(order, "CONFIRMED");
        orderEventService.broadcastOrderEvent(event);
    }

    public void shipOrder(Integer orderId) {
        Order order = findById(orderId);
        order.setStatus(OrderStatus.SHIPPED);
        orderRepo.save(order);
        createOrderTrack(order, OrderStatus.SHIPPED);

        OrderEvent event = createOrderEvent(order, "SHIPPED");
        orderEventService.broadcastOrderEvent(event);
    }

    public void deliverOrder(Integer orderId) {
        Order order = findById(orderId);
        order.setStatus(OrderStatus.DELIVERED);
        orderRepo.save(order);
        createOrderTrack(order, OrderStatus.DELIVERED);

        OrderEvent event = createOrderEvent(order, "DELIVERED");
        orderEventService.broadcastOrderEvent(event);
    }

    private void createOrderTrack(Order order, String status) {
        OrderTrack track = new OrderTrack();
        track.setOrder(order);
        track.setStatus(status);
        orderTrackRepo.save(track);
    }

    private OrderEvent createOrderEvent(Order order, String eventType) {
        OrderEvent event = new OrderEvent();
        event.setEventType(eventType);
        event.setOrderId(order.getId());
        event.setOrderNumber(order.getOrderNumber());
        event.setCustomerName(order.getCustomer().getFullName());
        event.setTotalAmount(order.getTotalAmount());
        event.setStatus(order.getStatus());
        return event;
    }

    private String generateOrderNumber() {
        return "ORD-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" +
                String.format("%03d", (int)(Math.random() * 1000));
    }
}