package org.shopping.site.admin.address;

import org.shopping.entity.Address;
import org.shopping.entity.Country;
import org.shopping.entity.State;
import org.shopping.entity.User;
import org.shopping.site.admin.BaseController;
import org.shopping.site.admin.country.CountryRepository;
import org.shopping.site.admin.state.StateRepository;
import org.shopping.site.admin.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.util.List;

@Controller
@RequestMapping("/addresses")
public class AddressController extends BaseController {

    @Autowired
    private AddressRepository addressRepo;

    @Autowired
    private CountryRepository countryRepo;

    @Autowired
    private StateRepository stateRepo;

    @Autowired
    private UserService userService;

    // List all addresses
    @GetMapping
    public String listAddresses(Model model, Authentication auth) {
        Integer userId = getCurrentUserId(auth);
        if (!userService.isCustomer(userId)) {
            return "redirect:/";
        }

        List<Address> addresses = addressRepo.findByUser_Id(userId);
        List<Country> countries = countryRepo.findAll();

        model.addAttribute("addresses", addresses);
        model.addAttribute("countries", countries);
        model.addAttribute("addressForm", new Address());
        return "addresses/list";
    }

    // Load states for country (AJAX)
    @GetMapping("/states/{countryId}")
    @ResponseBody
    public List<State> getStatesByCountry(@PathVariable Integer countryId) {
        return stateRepo.findByCountry_Id(countryId);
    }

    @PostMapping
    @Transactional
    public String saveAddress(
            @RequestParam Integer countryId,
            @RequestParam(required = false) Integer stateId,
            @Valid Address address,
            BindingResult bindingResult,
            Authentication auth,
            RedirectAttributes redirectAttributes,
            Model model) {

        Integer userId = getCurrentUserId(auth);
        if (!userService.isCustomer(userId)) {
            redirectAttributes.addFlashAttribute("error", "Only customers can manage addresses.");
            return "redirect:/";
        }

        // Handle validation errors - stay on form page
        if (bindingResult.hasErrors()) {
            model.addAttribute("addresses", addressRepo.findByUser_Id(userId));
            model.addAttribute("countries", countryRepo.findAll());
            model.addAttribute("addressForm", address);
            // Don't use redirectAttributes here - we're not redirecting
            return "addresses/list";
        }

        try {
            // Set user
            User user = userService.findById(userId);
            if (user == null) {
                throw new IllegalArgumentException("User not found");
            }
            address.setUser(user);

            // Set country
            Country country = countryRepo.findById(countryId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid country selection"));
            address.setCountry(country);

            // Set state (optional)
            if (stateId != null) {
                State state = stateRepo.findById(stateId)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid state selection"));
                address.setState(state);
            } else {
                address.setState(null);
            }

            // Handle default address logic
            if (Boolean.TRUE.equals(address.getIsDefault())) {
                addressRepo.unsetDefaultForUser(userId);
                address.setIsDefault(true);
            } else {
                address.setIsDefault(false);
            }

            // Save to database
            addressRepo.save(address);
            redirectAttributes.addFlashAttribute("message", "Address saved successfully!");

        } catch (IllegalArgumentException e) {
            // Handle validation/business logic errors
            redirectAttributes.addFlashAttribute("error", "Failed to save address: " + e.getMessage());
            return "redirect:/addresses";

        } catch (Exception e) {
            // Handle unexpected errors (database issues, etc.)
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred. Please try again.");
            return "redirect:/addresses";
        }

        return "redirect:/addresses";
    }

    // Edit form (prefill)
    @GetMapping("/edit/{id}")
    public String editAddress(@PathVariable Integer id,
                              Model model,
                              Authentication auth) {
        Integer userId = getCurrentUserId(auth);
        Address address = addressRepo.findById(id).orElse(null);

        if (address == null || !address.getUser().getId().equals(userId)) {
            return "redirect:/addresses";
        }

        model.addAttribute("addressForm", address);
        model.addAttribute("addresses", addressRepo.findByUser_Id(userId));
        model.addAttribute("countries", countryRepo.findAll());
        return "addresses/list";
    }

    // Delete address
    @PostMapping("/delete/{id}")
    public String deleteAddress(@PathVariable Integer id,
                                Authentication auth,
                                RedirectAttributes redirectAttributes) {
        Integer userId = getCurrentUserId(auth);
        Address address = addressRepo.findById(id).orElse(null);

        if (address != null && address.getUser().getId().equals(userId)) {
            addressRepo.deleteById(id);
            redirectAttributes.addFlashAttribute("message", "Address deleted.");
        }
        return "redirect:/addresses";
    }
}