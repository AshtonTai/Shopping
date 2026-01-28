package org.shopping.site.admin.cartitem;

import org.shopping.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    List<CartItem> findByCustomer_Id(Integer customerId);
    Optional<CartItem> findByCustomer_IdAndProduct_Id(Integer customerId, Integer productId);
    void deleteByCustomer_Id(Integer customerId);
}
