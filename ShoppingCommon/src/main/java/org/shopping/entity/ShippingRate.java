package org.shopping.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "shipping_rates")
@Data
@NoArgsConstructor
public class ShippingRate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;
    private BigDecimal cost;
    private Integer estimatedDays;

    @ManyToOne
    @JoinColumn(name = "country_id", nullable = false)
    private Country country;
}
