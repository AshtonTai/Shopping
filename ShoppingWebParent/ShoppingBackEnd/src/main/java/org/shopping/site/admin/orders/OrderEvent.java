package org.shopping.site.admin.orders;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderEvent {
    private String eventType; // "CREATED", "UPDATED", "DELIVERED"
    private Integer orderId;
    private String orderNumber;
    private String customerName;
    private BigDecimal totalAmount;
    private String status;
    private String deliveryStatus;
    private String role; // "CUSTOMER", "SHIPPER", "ADMIN"
}
