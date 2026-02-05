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
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;

@Controller
@RequestMapping("/addresses")
public class AddressController extends BaseController {

    @Autowired private AddressRepository addressRepo;
    @Autowired private CountryRepository countryRepo;
    @Autowired private StateRepository stateRepo;
    @Autowired private UserService userService;

    // ========== LIST ADDRESSES ==========
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
        model.addAttribute("addressForm", new Address()); // For create form
        return "addresses/list";
    }

    // ========== LOAD STATES (AJAX) ==========
    @GetMapping("/states/{countryId}")
    @ResponseBody
    public List<State> getStatesByCountry(@PathVariable Integer countryId) {
        if (countryId == null || countryId <= 0) {
            return List.of();
        }
        return stateRepo.findByCountry_Id(countryId);
    }

    // ========== CREATE ADDRESS (UNCHANGED - KEEP YOUR ORIGINAL) ==========
    @PostMapping
    @Transactional
    public String saveAddress(
            @RequestParam Integer countryId,
            @RequestParam(required = false) Integer stateId,
            @Valid Address address,
            BindingResult bindingResult,
            Authentication auth,
            RedirectAttributes redirectAttributes,
            Model model) { // ← Keep Model parameter for error handling

        Integer userId = getCurrentUserId(auth);
        if (!userService.isCustomer(userId)) {
            redirectAttributes.addFlashAttribute("error", "Only customers can manage addresses.");
            return "redirect:/";
        }

        // Handle validation errors - stay on form page (your original logic)
        if (bindingResult.hasErrors()) {
            model.addAttribute("addresses", addressRepo.findByUser_Id(userId));
            model.addAttribute("countries", countryRepo.findAll());
            model.addAttribute("addressForm", address);
            return "addresses/list"; // ← Return to list with form open
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
            redirectAttributes.addFlashAttribute("error", "Failed to save address: " + e.getMessage());
            return "redirect:/addresses";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred. Please try again.");
            return "redirect:/addresses";
        }

        return "redirect:/addresses";
    }

    // ========== SHOW EDIT FORM (UPDATED) ==========
    @GetMapping("/edit/{id}")
    public String showEditForm(
            @PathVariable Integer id,
            Model model,
            Authentication auth) {

        Integer userId = getCurrentUserId(auth);
        Address address = addressRepo.findById(id).orElse(null);

        if (address == null || !address.getUser().getId().equals(userId)) {
            return "redirect:/addresses";
        }

        model.addAttribute("address", address); // ← Key change: attribute name is "address"
        model.addAttribute("countries", countryRepo.findAll());
        return "addresses/edit"; // ← New dedicated edit page
    }

    // ========== UPDATE ADDRESS (NEW METHOD) ==========
    @PutMapping("/{id}")
    @Transactional
    public String updateAddress(
            @PathVariable Integer id,
            @RequestParam Integer countryId,
            @RequestParam(required = false) Integer stateId,
            @Valid Address addressForm,
            BindingResult bindingResult,
            Authentication auth,
            RedirectAttributes redirectAttributes) {

        Integer userId = getCurrentUserId(auth);
        Address existing = addressRepo.findById(id).orElse(null);

        if (existing == null || !existing.getUser().getId().equals(userId)) {
            redirectAttributes.addFlashAttribute("error", "Address not found or unauthorized.");
            return "redirect:/addresses";
        }

        // 🔒 SECURITY: Prevent country/state tampering
        if (!existing.getCountry().getId().equals(countryId) ||
                !Objects.equals(existing.getState() != null ? existing.getState().getId() : null, stateId)) {
            redirectAttributes.addFlashAttribute("error", "Country/State cannot be modified");
            return "redirect:/addresses/edit/" + id;
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("address", addressForm);
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.address",
                    bindingResult
            );
            return "redirect:/addresses/edit/" + id;
        }

        try {
            // Update editable fields
            existing.setFullName(addressForm.getFullName());
            existing.setPhone(addressForm.getPhone());
            existing.setAddressLine1(addressForm.getAddressLine1());
            existing.setAddressLine2(addressForm.getAddressLine2());
            existing.setCity(addressForm.getCity());
            existing.setPostalCode(addressForm.getPostalCode());
            existing.setIsDefault(addressForm.getIsDefault());

            // Country/State are preserved (already validated above)
            existing.setCountry(existing.getCountry()); // Reuse existing
            existing.setState(existing.getState());     // Reuse existing

            // Handle default logic
            if (Boolean.TRUE.equals(existing.getIsDefault())) {
                addressRepo.unsetDefaultForUser(userId);
                existing.setIsDefault(true);
            }

            addressRepo.save(existing);
            redirectAttributes.addFlashAttribute("message", "Address updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Update failed: " + e.getMessage());
            return "redirect:/addresses/edit/" + id;
        }

        return "redirect:/addresses";
    }

    // ========== DELETE ADDRESS (UNCHANGED) ==========
    @PostMapping("/delete/{id}")
    public String deleteAddress(
            @PathVariable Integer id,
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