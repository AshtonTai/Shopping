package org.shopping.site.admin.product.strategy;

import org.springframework.web.multipart.MultipartFile;

public record ProductSaveContext(
        MultipartFile mainImage,
        MultipartFile[] extraImages,
        String[] imageIDs,
        String[] imageNames,
        String[] detailIDs,
        String[] detailNames,
        String[] detailValues
) {}
