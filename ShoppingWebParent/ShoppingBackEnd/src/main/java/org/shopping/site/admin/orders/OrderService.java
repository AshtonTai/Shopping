package org.shopping.site.admin.orders;

import jakarta.transaction.Transactional;
import org.shopping.entity.*;
import org.shopping.entity.product.Product;
import org.shopping.site.admin.address.AddressRepository;
import org.shopping.site.admin.cartitem.CartItemRepository;
import org.shopping.site.admin.orders.constant.OrderStatus;
import org.shopping.site.admin.product.ProductRepository;
import org.shopping.site.admin.shippingrate.ShippingRateRepository;
import org.shopping.site.admin.voucher.VoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
public class OrderService {

    @Autowired private OrderRepository orderRepo;
    @Autowired private OrderDetailRepository orderDetailRepo;
    @Autowired private OrderTrackRepository orderTrackRepo;
    @Autowired private CartItemRepository cartItemRepo;
    @Autowired private ProductRepository productRepo;
    @Autowired private AddressRepository addressRepo;
    @Autowired private OrderEventService orderEventService;
    @Autowired private VoucherService voucherService;
    @Autowired private ShippingRateRepository shippingRateRepo;

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

        // Calculate subtotal (after product discounts)
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : cartItems) {
            Product product = item.getProduct();
            float unitPrice = product.getPrice();

            if (product.getDiscountPercent() > 0) {
                unitPrice = product.getDiscountPrice();
            }

            BigDecimal itemTotal = BigDecimal.valueOf(unitPrice)
                    .multiply(BigDecimal.valueOf(item.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            subtotal = subtotal.add(itemTotal);
        }

        BigDecimal shippingCost = BigDecimal.valueOf(10.00).setScale(2, RoundingMode.HALF_UP);
        if (address.getCountry() != null && address.getState() != null) {
            ShippingRate rate = shippingRateRepo.findByCountryAndState(address.getCountry(), address.getState());
            if (rate != null) {
                shippingCost = rate.getRate().setScale(2, RoundingMode.HALF_UP);
            }
        }

        BigDecimal voucherDiscount = calculateVoucherDiscount(subtotal, appliedVoucher);
        boolean isFreeShippingFromVoucher = (appliedVoucher != null && isFreeShippingVoucher(appliedVoucher));
        if (isFreeShippingFromVoucher) {
            shippingCost = BigDecimal.ZERO;
        }

        // Calculate final total
        BigDecimal finalTotal = subtotal
                .add(shippingCost)
                .subtract(voucherDiscount)
                .setScale(2, RoundingMode.HALF_UP);

        Order order = new Order();
        order.setCustomer(new User(customerId));
        order.setShippingAddress(address);
        order.setPaymentMethod(paymentMethod);
        order.setShippingCost(shippingCost);
        order.setDiscountAmount(voucherDiscount);
        order.setTotalAmount(finalTotal);
        order.setStatus(OrderStatus.PENDING);
        order.setOrderNumber(generateOrderNumber());

        Order savedOrder = orderRepo.save(order);

        if (appliedVoucher != null) {
            voucherService.recordVoucherUsage(appliedVoucher, customerId, savedOrder.getId());
        }

        for (CartItem item : cartItems) {
            Product product = item.getProduct();

            BigDecimal originalPrice = BigDecimal.valueOf(product.getPrice()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal unitPrice = (product.getDiscountPercent() > 0)
                    ? BigDecimal.valueOf(product.getDiscountPrice()).setScale(2, RoundingMode.HALF_UP)
                    : originalPrice;

            OrderDetail detail = new OrderDetail();
            detail.setOrder(savedOrder);
            detail.setProduct(product);
            detail.setQuantity(item.getQuantity());
            detail.setOriginalUnitPrice(originalPrice);
            detail.setUnitPrice(unitPrice);
            detail.setSubtotal(unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())).setScale(2, RoundingMode.HALF_UP));
            orderDetailRepo.save(detail);

            product.setInStock(product.getInStock() - item.getQuantity());
            productRepo.save(product);
        }

        cartItemRepo.deleteByCustomer_Id(customerId);
        createOrderTrack(savedOrder, OrderStatus.PENDING);
        OrderEvent event = createOrderEvent(savedOrder, "CREATED");
        orderEventService.broadcastOrderEvent(event);

        return savedOrder;
    }

    private BigDecimal calculateShippingCost(Address address, BigDecimal orderSubtotal) {
        BigDecimal freeShippingThreshold = BigDecimal.valueOf(100.00);
        if (orderSubtotal.compareTo(freeShippingThreshold) >= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(10.00).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateVoucherDiscount(BigDecimal subtotal, Voucher voucher) {
        if (voucher == null || voucher.getType() == null) return BigDecimal.ZERO;

        switch (voucher.getType()) {
            case PERCENTAGE:
                if (voucher.getValue() == null) return BigDecimal.ZERO;
                BigDecimal pct = voucher.getValue().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                return subtotal.multiply(pct).setScale(2, RoundingMode.HALF_UP).min(subtotal);

            case FIXED_AMOUNT:
                if (voucher.getValue() == null) return BigDecimal.ZERO;
                return voucher.getValue().min(subtotal).setScale(2, RoundingMode.HALF_UP);

            case FREE_SHIPPING:
                return BigDecimal.ZERO;

            default:
                return BigDecimal.ZERO;
        }
    }

    private boolean isFreeShippingVoucher(Voucher voucher) {
        if (voucher == null || voucher.getType() == null) return false;
        return voucher.getType() == VoucherType.FREE_SHIPPING;
    }

    public void confirmOrder(Integer orderId) {
        Order order = findById(orderId);
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepo.save(order);
        createOrderTrack(order, OrderStatus.CONFIRMED);
        orderEventService.broadcastOrderEvent(createOrderEvent(order, "CONFIRMED"));
    }

    public void shipOrder(Integer orderId) {
        Order order = findById(orderId);
        order.setStatus(OrderStatus.SHIPPED);
        orderRepo.save(order);
        createOrderTrack(order, OrderStatus.SHIPPED);
        orderEventService.broadcastOrderEvent(createOrderEvent(order, "SHIPPED"));
    }

    public void deliverOrder(Integer orderId) {
        Order order = findById(orderId);
        order.setStatus(OrderStatus.DELIVERED);
        orderRepo.save(order);
        createOrderTrack(order, OrderStatus.DELIVERED);
        orderEventService.broadcastOrderEvent(createOrderEvent(order, "DELIVERED"));
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
        event.setStatus(order.getStatus().toString());
        return event;
    }

    public String generateOrderNumber() {
        return "ORD-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" +
                String.format("%04d", (int)(Math.random() * 10000));
    }
}