package org.shopping.site.admin.orders;

import org.shopping.entity.Order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;


import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Integer> {
    List<Order> findByCustomer_Id(Integer customerId);
    @Query("SELECT o FROM Order o WHERE o.customer.id = :customerId ORDER BY o.orderTime DESC")
    Page<Order> findByCustomer_IdOrderByOrderTimeDesc(Integer customerId, Pageable pageable);
}
