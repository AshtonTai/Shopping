package org.shopping.site.admin.review;

import org.shopping.entity.review.Review;
import org.shopping.site.admin.paging.PagingAndSortingHelper;
import org.shopping.site.admin.paging.PagingAndSortingParam;
import org.shopping.site.admin.security.ShoppingUserDetails;
import org.shopping.site.admin.util.FileUploadUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class ReviewController {

    private static final String DEFAULT_REDIRECT_URL = "redirect:/reviews/page/1?sortField=id&sortDir=asc";

    @Autowired
    private ReviewService reviewService;

    @GetMapping("/reviews")
    public String listFirstPage(Model model) {
        return DEFAULT_REDIRECT_URL;
    }

    @GetMapping("/reviews/page/{pageNum}")
    public String listByPage(
            @PagingAndSortingParam(listName = "listReviews", moduleURL = "/reviews")
            PagingAndSortingHelper helper,
            @PathVariable int pageNum,
            Model model) {

        List<String> allowedSortFields = List.of("id", "rating", "reviewTime");
        Page<Review> page = reviewService.listAll(
                helper.getKeyword(),
                helper.createPageable(10, pageNum, allowedSortFields)
        );
        long startCount = (pageNum - 1L) * 10 + 1;
        long endCount = Math.min(startCount + 10 - 1, page.getTotalElements());

        String sortField = helper.getSortField();
        String sortDir = helper.getSortDir();
        String reverseSortDir = sortDir.equals("asc") ? "desc" : "asc";

        model.addAttribute("moduleURL", "/reviews");
        model.addAttribute("currentPage", pageNum);
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", reverseSortDir);
        model.addAttribute("keyword", helper.getKeyword());

        model.addAttribute("listReviews", page.getContent());
        model.addAttribute("totalPages", page.getTotalPages());
        model.addAttribute("totalItems", page.getTotalElements());
        model.addAttribute("startCount", startCount);
        model.addAttribute("endCount", endCount);

        return "reviews/reviews";
    }

    @GetMapping("/reviews/detail/{id}")
    public String viewReviewDetails(@PathVariable("id") Integer id, Model model) {
        Review review = reviewService.get(id);
        model.addAttribute("review", review);
        return "reviews/review_detail_modal";
    }

    @GetMapping("/reviews/delete/{id}")
    public String deleteReview(@PathVariable("id") Integer id, RedirectAttributes ra) {
        try {
            reviewService.delete(id);
            ra.addFlashAttribute("message", "The review ID " + id + " has been deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("message", e.getMessage());
        }
        return DEFAULT_REDIRECT_URL;
    }

    @GetMapping("/reviews/edit/{id}")
    public String editReview(@PathVariable("id") Integer id, Model model) {
        Review review = reviewService.get(id);
        model.addAttribute("review", review);
        model.addAttribute("pageTitle", "Edit Review (ID: " + id + ")");
        return "reviews/review_form";
    }

    @PostMapping("/reviews/save")
    public String saveReview(Review review, RedirectAttributes ra,
                             @AuthenticationPrincipal ShoppingUserDetails loggedUser) {
        try {
            reviewService.save(review, loggedUser);
            ra.addFlashAttribute("message", "The review has been saved successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return DEFAULT_REDIRECT_URL;
    }
}