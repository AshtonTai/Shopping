package org.shopping.site.admin.product.strategy;

import org.shopping.entity.product.Product;
import org.shopping.site.admin.security.ShoppingUserDetails;

public interface ProductSaveStrategy {
    boolean supports(ShoppingUserDetails user);
    void save(Product product, ProductSaveContext context);
}
 