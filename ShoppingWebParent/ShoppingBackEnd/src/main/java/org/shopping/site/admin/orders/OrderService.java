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
        return createOrder(customerId, addressId, paymentMethod, cartItems, null);
    }

    public Order createOrder(Integer customerId, Integer addressId, String paymentMethod,
                             List<CartItem> cartItems, Voucher appliedVoucher) {
        Address address = addressRepo.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        if (address.getCountry() == null) {
            throw new RuntimeException("Address must have a country");
        }

        // Calculate subtotal from cart items
        BigDecimal subtotal = cartItems.stream()
                .map(item -> {
                    float price = item.getProduct().getPrice();
                    int quantity = item.getQuantity();
                    return BigDecimal.valueOf(price).multiply(BigDecimal.valueOf(quantity));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate discount amount
        BigDecimal discountAmount = calculateDiscount(subtotal, appliedVoucher);
        BigDecimal discountedSubtotal = subtotal.subtract(discountAmount);

        // Calculate shipping cost (free shipping if voucher applies)
        BigDecimal shippingCost = calculateShippingCost(address, appliedVoucher);

        // Calculate final total
        BigDecimal total = discountedSubtotal.add(shippingCost);

        Order order = new Order();
        order.setCustomer(new User(customerId));
        order.setShippingAddress(address);
        order.setPaymentMethod(paymentMethod);
        order.setTotalAmount(total);
        order.setShippingCost(shippingCost);
        order.setStatus(OrderStatus.PENDING);
        order.setOrderNumber(generateOrderNumber());
        order.setAppliedVoucher(appliedVoucher);
        order.setDiscountAmount(discountAmount);
        order.setFreeShippingApplied(appliedVoucher != null &&
                appliedVoucher.getType() == VoucherType.FREE_SHIPPING);

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

    private BigDecimal calculateDiscount(BigDecimal subtotal, Voucher voucher) {
        if (voucher == null || voucher.getType() == VoucherType.FREE_SHIPPING) {
            return BigDecimal.ZERO;
        }

        switch (voucher.getType()) {
            case PERCENTAGE:
                BigDecimal percentage = voucher.getValue().min(BigDecimal.valueOf(100));
                return subtotal.multiply(percentage).divide(BigDecimal.valueOf(100));
            case FIXED_AMOUNT:
                return voucher.getValue().min(subtotal); // Don't exceed subtotal
            default:
                return BigDecimal.ZERO;
        }
    }

    private BigDecimal calculateShippingCost(Address address, Voucher voucher) {
        if (voucher != null && voucher.getType() == VoucherType.FREE_SHIPPING) {
            return BigDecimal.ZERO;
        }

        ShippingRate rate = shippingRateRepo.findByCountryAndState(
                address.getCountry(),
                address.getState()
        );

        return rate != null ? rate.getRate() : BigDecimal.ZERO;
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

    public String generateOrderNumber() { // ← Make sure it's public
        return "ORD-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" +
                String.format("%03d", (int)(Math.random() * 1000));
    }
}