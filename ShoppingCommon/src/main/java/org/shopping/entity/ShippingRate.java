package org.shopping.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "shipping_rates")
public class ShippingRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "country_id", nullable = false)
    private Country country;

    @ManyToOne
    @JoinColumn(name = "state_id", nullable = false)
    private State state;


    private BigDecimal rate;
    private Integer days;
    private boolean codSupported;

    public Integer getId() { return id; }
    public Country getCountry() { return country; }
    public State getState() { return state; }
    public BigDecimal getRate() { return rate; }
    public Integer getDays() { return days; }
    public boolean isCodSupported() { return codSupported; }

    // Setters (optional but good for JPA)
    public void setId(Integer id) { this.id = id; }
    public void setCountry(Country country) { this.country = country; }
    public void setState(State state) { this.state = state; }
    public void setRate(BigDecimal rate) { this.rate = rate; }
    public void setDays(Integer days) { this.days = days; }
    public void setCodSupported(boolean codSupported) { this.codSupported = codSupported; }
}
