package org.shopping.site.admin.product.strategy;

import org.shopping.entity.product.Product;
import org.shopping.site.admin.product.ProductService;
import org.shopping.site.admin.security.ShoppingUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PriceOnlyProductSaveStrategy implements ProductSaveStrategy {
    @Autowired
    private ProductService productService;

    @Override
    public boolean supports(ShoppingUserDetails user) {
        return user.hasRole("Salesperson");
    }

    @Override
    public void save(Product product, ProductSaveContext ctx) {
        productService.saveProductPrice(product); // only updates price fields
    }
}
