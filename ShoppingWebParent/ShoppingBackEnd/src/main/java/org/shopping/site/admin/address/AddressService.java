package org.shopping.site.admin.address;

import org.shopping.entity.Address;
import jakarta.transaction.Transactional;
import org.shopping.site.admin.country.CountryRepository;
import org.shopping.site.admin.state.StateRepository;
import org.shopping.site.admin.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class AddressService {

    @Autowired
    private AddressRepository addressRepo;
    @Autowired private CountryRepository countryRepo;
    @Autowired private StateRepository stateRepo;
    @Autowired private UserService userService;

    public Address saveAddress(Address address, Integer userId, Integer countryId, Integer stateId) {
        // Set user
        address.setUser(userService.findById(userId));

        // Set country/state
        address.setCountry(countryRepo.findById(countryId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid country")));

        if (stateId != null) {
            address.setState(stateRepo.findById(stateId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid state")));
        } else {
            address.setState(null);
        }

        // Handle default address
        if (Boolean.TRUE.equals(address.getIsDefault())) {
            addressRepo.unsetDefaultForUser(userId); // Now runs in transaction
            address.setIsDefault(true);
        } else {
            address.setIsDefault(false);
        }

        return addressRepo.save(address);
    }
}
