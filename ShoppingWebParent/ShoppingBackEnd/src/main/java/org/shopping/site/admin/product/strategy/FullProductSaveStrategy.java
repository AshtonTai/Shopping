package org.shopping.site.admin.product.strategy;

import org.shopping.entity.product.Product;
//import org.shopping.site.admin.product.strategy.ProductSaveContext;
import org.shopping.site.admin.product.ProductService;
import org.shopping.site.admin.product.service.ProductImageService;
import org.shopping.site.admin.security.ShoppingUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FullProductSaveStrategy implements ProductSaveStrategy {

    @Autowired
    private ProductImageService imageService; // ← renamed from helper

    @Autowired
    private ProductService productService;

    @Override
    public boolean supports(ShoppingUserDetails user) {
        return user.hasRole("Admin") || user.hasRole("Editor");
    }

    @Override
    public void save(Product product, ProductSaveContext ctx) {
        imageService.setMainImageName(ctx.mainImage(), product);
        imageService.setExistingExtraImageNames(ctx.imageIDs(), ctx.imageNames(), product);
        imageService.setNewExtraImageNames(ctx.extraImages(), product);
        imageService.setProductDetails(ctx.detailIDs(), ctx.detailNames(), ctx.detailValues(), product);

        Product saved = productService.save(product);
        try {
            imageService.saveUploadedImages(ctx.mainImage(), ctx.extraImages(), saved);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save product images", e);
        }

        imageService.deleteExtraImagesWiredRemovedOnForm(product); // ← correct method name!
    }
}