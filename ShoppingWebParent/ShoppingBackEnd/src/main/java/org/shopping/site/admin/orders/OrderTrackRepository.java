package org.shopping.site.admin.orders;

import org.shopping.entity.OrderTrack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderTrackRepository extends JpaRepository<OrderTrack, Integer> {
    List<OrderTrack> findByOrder_IdOrderByUpdatedTimeAsc(Integer orderId);
}
