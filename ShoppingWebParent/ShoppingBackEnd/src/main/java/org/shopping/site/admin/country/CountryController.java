package org.shopping.site.admin.country;

import org.shopping.entity.Country;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/countries")
@PreAuthorize("hasAuthority('Admin')")
public class CountryController {

    @Autowired private CountryRepository countryRepo;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("countries", countryRepo.findAll());
        return "country/countries";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("country", new Country());
        return "country/countries_form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model) {
        model.addAttribute("country", countryRepo.findById(id).orElse(new Country()));
        return "country/countries_form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Country country) {
        countryRepo.save(country);
        return "redirect:/admin/countries";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        countryRepo.deleteById(id);
        return "redirect:/admin/countries";
    }
}
