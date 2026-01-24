package org.shopping.site.admin.review;

import org.shopping.entity.review.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Integer> {

    Page<Review> findAll(Pageable pageable);

    Optional<Review> findByUser_IdAndProduct_Id(Integer userId, Integer productId);

    Page<Review> findByProduct_Id(Integer productId, Pageable pageable);
    Page<Review> findByUser_Id(Integer userId, Pageable pageable);
    List<Review> findByProduct_Id(Integer productId);

    @Query("SELECT r FROM Review r WHERE r.headline LIKE %:keyword% OR r.comment LIKE %:keyword%")
    Page<Review> findByKeyword(@Param("keyword") String keyword, Pageable pageable);
}