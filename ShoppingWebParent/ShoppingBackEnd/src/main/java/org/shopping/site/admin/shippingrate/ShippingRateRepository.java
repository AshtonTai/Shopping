package org.shopping.site.admin.shippingrate;

import org.shopping.entity.ShippingRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShippingRateRepository extends JpaRepository<ShippingRate, Integer> {
    List<ShippingRate> findByCountry_Id(Integer countryId);
}
