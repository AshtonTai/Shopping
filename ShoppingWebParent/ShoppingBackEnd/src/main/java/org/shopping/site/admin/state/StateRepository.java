package org.shopping.site.admin.state;

import org.shopping.entity.State;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StateRepository extends JpaRepository<State, Integer> {
    List<State> findByCountry_Id(Integer countryId);
}
