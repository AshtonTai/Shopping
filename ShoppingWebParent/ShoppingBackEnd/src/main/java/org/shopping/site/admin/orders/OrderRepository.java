package org.shopping.site.admin.orders;

import org.shopping.entity.Order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.time.LocalDate;
import java.util.List;

// OrderRepository.java
public interface OrderRepository extends JpaRepository<Order, Integer> {
    List<Order> findByCustomer_Id(Integer customerId);

    @Query("SELECT o FROM Order o WHERE o.customer.id = :customerId ORDER BY o.orderTime DESC")
    Page<Order> findByCustomer_IdOrderByOrderTimeDesc(Integer customerId, Pageable pageable);

    // Updated to use status field only
    List<Order> findByStatus(String status);

    @Query("SELECT o FROM Order o WHERE DATE(o.orderTime) = :date ORDER BY o.orderTime DESC")
    List<Order> findByOrderDate(@Param("date") LocalDate date);

    @Query("SELECT o FROM Order o WHERE o.status = :status AND DATE(o.orderTime) = :date")
    List<Order> findByStatusAndOrderDate(@Param("status") String status, @Param("date") LocalDate date);

    @Query("SELECT o FROM Order o ORDER BY o.orderTime DESC")
    List<Order> findTop10ByOrderByOrderTimeDesc();
}
