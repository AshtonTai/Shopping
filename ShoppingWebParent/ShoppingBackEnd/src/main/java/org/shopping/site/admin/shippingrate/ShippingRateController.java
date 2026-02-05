package org.shopping.site.admin.shippingrate;

import org.shopping.entity.Country;
import org.shopping.entity.ShippingRate;
import org.shopping.entity.State;
import org.shopping.site.admin.country.CountryRepository;
import org.shopping.site.admin.state.StateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/shipping-rates")
@PreAuthorize("hasAnyAuthority('Admin','Editor')")
public class ShippingRateController {

    @Autowired private ShippingRateRepository shippingRateRepo;
    @Autowired private CountryRepository countryRepo;
    @Autowired private StateRepository stateRepository;

    @GetMapping
    public String listRates(
            Model model,
            @RequestParam(required = false) Integer editId
    ) {
        model.addAttribute("shippingRates", shippingRateRepo.findAll());
        model.addAttribute("countries", countryRepo.findAll());

        ShippingRate rate = (editId != null)
                ? shippingRateRepo.findById(editId).orElse(new ShippingRate())
                : new ShippingRate();

        model.addAttribute("newShippingRate", rate);
        model.addAttribute("editing", editId != null);

        // Only country pre-selection needed
        if (editId != null && rate.getCountry() != null) {
            model.addAttribute("selectedCountryId", rate.getCountry().getId());
        } else {
            model.addAttribute("selectedCountryId", null);
        }

        return "shipping_rates/shipping_rates";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(
            @PathVariable Integer id,
            Model model) {

        ShippingRate rate = shippingRateRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid shipping rate ID"));

        model.addAttribute("shippingRate", rate);
        model.addAttribute("countries", countryRepo.findAll());
        return "shipping_rates/edit";
    }

    @PostMapping("/update/{id}")
    public String updateRate(
            @PathVariable Integer id,
            @ModelAttribute("shippingRate") ShippingRate updatedRate, // ← Key change
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Validation failed");
            return "redirect:/shipping-rates/edit/" + id;
        }

        ShippingRate existing = shippingRateRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid shipping rate ID"));

        existing.setCountry(updatedRate.getCountry());
        existing.setRate(updatedRate.getRate());
        existing.setDays(updatedRate.getDays());
        existing.setCodSupported(updatedRate.isCodSupported());

        shippingRateRepo.save(existing);
        redirectAttributes.addFlashAttribute("message", "Shipping rate updated successfully!");
        return "redirect:/shipping-rates";
    }

    @PostMapping("/save")
    public String saveRate(
            @RequestParam(required = false) Integer id,
            @RequestParam Integer countryId,
            @RequestParam Integer stateId,
            @RequestParam BigDecimal rate,
            @RequestParam Integer days,
            @RequestParam(defaultValue = "false") boolean codSupported) {

        Country country = countryRepo.findById(countryId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid country"));
        State state =stateRepository.findById(stateId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid state"));

        ShippingRate shippingRate = (id != null)
                ? shippingRateRepo.findById(id).orElse(new ShippingRate())
                : new ShippingRate();

        shippingRate.setCountry(country);
        shippingRate.setState(state);
        shippingRate.setRate(rate);
        shippingRate.setDays(days);
        shippingRate.setCodSupported(codSupported);

        shippingRateRepo.save(shippingRate);
        return "redirect:/shipping-rates";
    }

    @PostMapping("/delete/{id}")
    public String deleteRate(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        shippingRateRepo.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "Shipping rate deleted successfully!");
        return "redirect:/shipping-rates";
    }
}