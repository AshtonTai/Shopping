package org.shopping.site.admin.product;

import jakarta.transaction.Transactional;
import org.shopping.entity.product.Product;
import org.shopping.exeption.ProductNotFoundException;
import org.shopping.site.admin.paging.PagingAndSortingHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class ProductService {
    public static final int PRODUCTS_PER_PAGE = 5;

    @Autowired
    private ProductRepository repo;

    public List<Product> listAll() {
        return (List<Product>) repo.findAll();
    }

    public Page<Product> listByPage(int pageNum, PagingAndSortingHelper helper, Integer categoryId) {
        // ✅ Define allowed sort fields for Product
        List<String> allowedSortFields = List.of("id", "name", "price", "enabled", "createdTime");

        Pageable pageable = helper.createPageable(PRODUCTS_PER_PAGE, pageNum, allowedSortFields);
        String keyword = helper.getKeyword();

        if (keyword != null && !keyword.isEmpty()) {
            if (categoryId != null && categoryId > 0) {
                String categoryIdMatch = "-" + categoryId + "-";
                return repo.searchInCategory(categoryId, categoryIdMatch, keyword, pageable);
            } else {
                return repo.findAll(keyword, pageable);
            }
        } else {
            if (categoryId != null && categoryId > 0) {
                String categoryIdMatch = "-" + categoryId + "-";
                return repo.findAllInCategory(categoryId, categoryIdMatch, pageable);
            } else {
                return repo.findAll(pageable);
            }
        }
    }

    public Page<Product> searchProducts(int pageNum, PagingAndSortingHelper helper) {
        List<String> allowedSortFields = List.of("id", "name", "price", "enabled", "createdTime");
        Pageable pageable = helper.createPageable(PRODUCTS_PER_PAGE, pageNum, allowedSortFields);
        String keyword = helper.getKeyword();
        return repo.searchProductsByName(keyword, pageable);
    }

    public Product save(Product product) {
        if (product.getId() == null) {
            product.setCreatedTime(new Date());
        }

        if (product.getAlias() == null || product.getAlias().isEmpty()) {
            String defaultAlias = product.getName().replaceAll(" ", "-");
            product.setAlias(defaultAlias);
        } else {
            product.setAlias(product.getAlias().replaceAll(" ", "-"));
        }

        product.setUpdatedTime(new Date());

        Product updatedProduct = repo.save(product);
//        repo.updateReviewCountAndAverageRating(updatedProduct.getId());

        return updatedProduct;
    }

    public void saveProductPrice(Product productInForm) {
        Product productInDB = repo.findById(productInForm.getId()).get();
        productInDB.setCost(productInForm.getCost());
        productInDB.setPrice(productInForm.getPrice());
        productInDB.setDiscountPercent(productInForm.getDiscountPercent());

        repo.save(productInDB);
    }

    public String checkUnique(Integer id, String name) {
        boolean isCreatingNew = (id == null || id == 0);
        Product productByName = repo.findByName(name);

        if (isCreatingNew) {
            if (productByName != null) return "Duplicate";
        } else {
            if (productByName != null && productByName.getId() != id) {
                return "Duplicate";
            }
        }

        return "OK";
    }

    public void updateProductEnabledStatus(Integer id, boolean enabled) {
        repo.updateEnabledStatus(id, enabled);
    }

    public void delete(Integer id) throws ProductNotFoundException {
        Long countById = repo.countById(id);

        if (countById == null || countById == 0) {
            throw new ProductNotFoundException("Could not find any product with ID " + id);
        }

        repo.deleteById(id);
    }

    public Product get(Integer id) throws ProductNotFoundException {
        try {
            return repo.findById(id).get();
        } catch (NoSuchElementException ex) {
            throw new ProductNotFoundException("Could not find any product with ID " + id);
        }
    }
}