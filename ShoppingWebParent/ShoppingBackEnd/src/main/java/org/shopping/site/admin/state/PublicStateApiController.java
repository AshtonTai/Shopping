package org.shopping.site.admin.state;

import org.shopping.entity.State;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PublicStateApiController {

    @Autowired
    private StateRepository stateRepo;

    @GetMapping("/states")
    public List<State> getStatesByCountry(@RequestParam("countryId") Integer countryId) {
        if (countryId == null || countryId <= 0) {
            return List.of();
        }
        return stateRepo.findByCountry_Id(countryId);
    }
}