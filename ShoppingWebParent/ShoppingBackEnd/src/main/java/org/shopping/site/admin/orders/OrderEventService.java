package org.shopping.site.admin.orders;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderEventService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Broadcast to all users (admins, shippers, customers)
    public void broadcastOrderEvent(OrderEvent event) {
        messagingTemplate.convertAndSend("/topic/orders", event);
    }

    // Send to specific user
    public void sendToUser(String userId, OrderEvent event) {
        messagingTemplate.convertAndSendToUser(userId, "/queue/orders", event);
    }

    // Send to specific role
    public void sendToRole(String role, OrderEvent event) {
        messagingTemplate.convertAndSend("/topic/orders/" + role.toLowerCase(), event);
    }
}
