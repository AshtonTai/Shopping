package org.shopping.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true)
    private String orderNumber; // e.g., ORD-20260127-001

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @CreationTimestamp
    private LocalDateTime orderTime;

    private String paymentMethod; // "CASH_ON_DELIVERY", etc.
    private BigDecimal totalAmount;
    private BigDecimal shippingCost;
    private String status; // PENDING, SHIPPED, etc.

    @ManyToOne
    @JoinColumn(name = "shipping_address_id", nullable = false)
    private Address shippingAddress;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderDetail> orderDetails = new ArrayList<>();
}
