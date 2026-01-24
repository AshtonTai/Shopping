package org.shopping.entity.review;

import jakarta.persistence.*;
import lombok.*;
import org.shopping.entity.product.Product;
import org.shopping.entity.User;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 150)
    private String headline;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String comment;

    @Column(nullable = false)
    private Byte rating; // 1 to 5

    @Column(name = "review_time", nullable = false, updatable = false)
    private LocalDateTime reviewTime = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Ensure one review per user per product
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Review)) return false;
        Review review = (Review) o;
        return Objects.equals(id, review.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
