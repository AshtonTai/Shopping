package org.shopping.site.admin.review;

import org.shopping.entity.User;
import org.shopping.entity.review.Review;
import org.shopping.exeption.ReviewNotFoundException;
import org.shopping.site.admin.product.ProductService;
import org.shopping.site.admin.security.ShoppingUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class ReviewService {

    @Autowired
    private ReviewRepository repo;

    @Autowired
    private ProductService productService;

    public Page<Review> listAll(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.isEmpty()) {
            return repo.findByKeyword(keyword, pageable);
        }
        return repo.findAll(pageable);
    }

    public Page<Review> listAllPublic(Pageable pageable) {
        return repo.findAll(pageable);
    }

    public void save(Review review, ShoppingUserDetails loggedUser) {
        if (!loggedUser.hasRole("Customer")) {
            throw new IllegalStateException("Only customers can submit reviews.");
        }

        if (review.getRating() < 1 || review.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5.");
        }

        // Prevent duplicate review
        if (repo.findByUser_IdAndProduct_Id(loggedUser.getId(), review.getProduct().getId()).isPresent()) {
            throw new IllegalStateException("You have already reviewed this product.");
        }

        // Set user
        User user = new User();
        user.setId(loggedUser.getId());
        review.setUser(user);

        // Save review
        Review savedReview = repo.save(review);

        try {
            productService.recalculateReviewStats(savedReview.getProduct().getId());
        } catch (Exception e) {
            // If product doesn't exist, delete the orphaned review and rethrow
            repo.delete(savedReview);
            throw new IllegalStateException("Failed to update product ratings: " + e.getMessage(), e);
        }
    }

    public void delete(Integer id, Integer currentUserId) {
        Review review = repo.findById(id)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with ID: " + id));

        // 🔒 Enforce ownership
        if (!review.getUser().getId().equals(currentUserId)) {
            throw new SecurityException("You can only delete your own reviews.");
        }

        Integer productId = review.getProduct().getId();
        repo.delete(review);

        try {
            productService.recalculateReviewStats(productId);
        } catch (Exception e) {
            System.err.println("Warning: Could not update ratings for product ID " + productId + " after review deletion: " + e.getMessage());
        }
    }

    public void delete(Integer id) {
        Review review = repo.findById(id)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with ID: " + id));

        Integer productId = review.getProduct().getId();
        repo.delete(review);

        try {
            productService.recalculateReviewStats(productId);
        } catch (Exception e) {
            // Log or ignore — product might have been deleted separately
            System.err.println("Warning: Could not update ratings for product ID " + productId + " after review deletion: " + e.getMessage());
        }
    }

    public Review get(Integer id) {
        return repo.findById(id)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with ID: " + id));
    }

    public boolean hasReviewedProduct(Integer userId, Integer productId) {
        return repo.findByUser_IdAndProduct_Id(userId, productId).isPresent();
    }
}