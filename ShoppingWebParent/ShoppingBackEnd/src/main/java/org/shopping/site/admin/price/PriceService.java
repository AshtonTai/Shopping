package org.shopping.site.admin.price;

import org.shopping.entity.CartItem;
import org.shopping.entity.product.Product;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
public class PriceService {

    public BigDecimal getProductFinalPrice(Product product) {
        if (product == null) {
            return BigDecimal.ZERO;
        }

        if (product.getDiscountPercent() > 0) {
            return BigDecimal.valueOf(product.getDiscountPrice());
        }

        return BigDecimal.valueOf(product.getPrice());
    }

    public BigDecimal getProductOriginalPrice(Product product) {
        if (product == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(product.getPrice());
    }

    public BigDecimal calculateCartSubtotal(List<CartItem> cartItems) {
        return cartItems.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getProduct() != null)
                .map(item -> {
                    BigDecimal finalPrice = getProductFinalPrice(item.getProduct());
                    return finalPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
