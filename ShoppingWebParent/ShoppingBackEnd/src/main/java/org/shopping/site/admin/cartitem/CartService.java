package org.shopping.site.admin.cartitem;

import jakarta.transaction.Transactional;
import org.shopping.entity.CartItem;
import org.shopping.entity.User;
import org.shopping.entity.product.Product;
import org.shopping.site.admin.product.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CartService {

    @Autowired
    private CartItemRepository cartItemRepo;
    @Autowired private ProductRepository productRepo;

    public void addToCart(Integer customerId, Integer productId, Integer quantity) {
        // Validate inputs
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (product.getInStock() < quantity) {
            throw new RuntimeException("Not enough stock available. Only " + product.getInStock() + " left.");
        }

        // Check if item already exists in cart
        Optional<CartItem> existingItem = cartItemRepo.findByCustomer_IdAndProduct_Id(customerId, productId);
        if (existingItem.isPresent()) {
            // Update quantity
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
            cartItemRepo.save(item);
        } else {
            // Create new cart item
            CartItem newItem = new CartItem();
            newItem.setCustomer(new User(customerId));
            newItem.setProduct(product);
            newItem.setQuantity(quantity);
            cartItemRepo.save(newItem);
        }
    }

    public List<CartItem> getCartItems(Integer customerId) {
        return cartItemRepo.findByCustomer_Id(customerId);
    }

    public void removeFromCart(Integer cartItemId) {
        cartItemRepo.deleteById(cartItemId);
    }

    public void clearCart(Integer customerId) {
        cartItemRepo.deleteByCustomer_Id(customerId);
    }
}
