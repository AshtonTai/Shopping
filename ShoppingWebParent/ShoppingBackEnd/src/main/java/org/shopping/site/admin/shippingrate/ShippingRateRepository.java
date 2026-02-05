package org.shopping.site.admin.shippingrate;

import org.shopping.entity.Country;
import org.shopping.entity.ShippingRate;
import org.shopping.entity.State;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShippingRateRepository extends JpaRepository<ShippingRate, Integer> {
    ShippingRate findByCountry(Country country);

    ShippingRate findByCountry_Id(Integer countryId);

    @Query("SELECT sr FROM ShippingRate sr WHERE sr.country = :country AND sr.state = :state")
    ShippingRate findByCountryAndState(@Param("country") Country country, @Param("state") State state);
}