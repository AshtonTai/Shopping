package org.shopping.site.admin.address;

import org.shopping.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AddressRepository extends JpaRepository<Address, Integer> {
    List<Address> findByUser_Id(Integer userId);
}
