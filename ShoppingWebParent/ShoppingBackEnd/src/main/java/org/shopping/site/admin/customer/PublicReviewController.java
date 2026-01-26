package org.shopping.site.admin.customer;

import org.shopping.entity.User;
import org.shopping.entity.product.Product;
import org.shopping.entity.review.Review;
import org.shopping.site.admin.review.ReviewService;
import org.shopping.site.admin.security.ShoppingUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PublicReviewController {

    @Autowired
    private ReviewService reviewService;

    @GetMapping("/reviews/public")
    public String listPublicReviews(
            @RequestParam(defaultValue = "1") int page,
            Model model,
            @AuthenticationPrincipal ShoppingUserDetails loggedUser) { // ← Add this

        Page<Review> reviewsPage = reviewService.listAllPublic(
                PageRequest.of(page - 1, 10, Sort.by("reviewTime").descending())
        );

        Integer currentUserId = null;
        if (loggedUser != null) {
            currentUserId = loggedUser.getId();
        }

        model.addAttribute("listReviews", reviewsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", reviewsPage.getTotalPages());
        model.addAttribute("currentUserId", currentUserId); // ← Critical!

        return "reviews/public_reviews";
    }

    @PostMapping("/reviews/submit")
    public String submitReview(
            @RequestParam Integer productId,
            @RequestParam Byte rating,
            @RequestParam(required = false) String headline,
            @RequestParam String comment,
            RedirectAttributes ra,
            Authentication authentication) {

        try {
            ShoppingUserDetails userDetails = (ShoppingUserDetails) authentication.getPrincipal();

            if (!userDetails.hasRole("Customer")) {
                ra.addFlashAttribute("error", "Only customers can submit reviews.");
                return "redirect:/products/detail/" + productId;
            }

            Review review = new Review();

            Product product = new Product();
            product.setId(productId);
            review.setProduct(product);

            User user = new User();
            user.setId(userDetails.getId());
            review.setUser(user);

            review.setRating(rating);
            review.setHeadline(headline);
            review.setComment(comment);

            reviewService.save(review, userDetails);
            ra.addFlashAttribute("message", "Thank you for your review!");

        } catch (Exception e) {
            e.printStackTrace(); // ← Add this for debugging
            ra.addFlashAttribute("error", "Failed to submit review. Please try again.");
        }

        return "redirect:/products/detail/" + productId;
    }

    @GetMapping("/reviews/public/delete/{id}")
    public String deleteOwnReview(
            @PathVariable Integer id,
            @AuthenticationPrincipal ShoppingUserDetails user,
            RedirectAttributes ra) {

        if (user == null) {
            ra.addFlashAttribute("error", "You must be logged in.");
            return "redirect:/reviews/public";
        }

        try {
            reviewService.delete(id, user.getId()); // ← Call new method with user ID
            ra.addFlashAttribute("message", "Your review has been deleted.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/reviews/public";
    }
}