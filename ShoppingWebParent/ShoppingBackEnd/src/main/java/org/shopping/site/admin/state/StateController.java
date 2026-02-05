package org.shopping.site.admin.state;

import org.shopping.entity.Country;
import org.shopping.entity.State;
import org.shopping.site.admin.country.CountryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/states")
@PreAuthorize("hasAuthority('Admin')")
public class StateController {

    @Autowired private StateRepository stateRepo;
    @Autowired private CountryRepository countryRepo;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("states", stateRepo.findAll());
        model.addAttribute("countries", countryRepo.findAll());
        return "state/states";  // ← maps to templates/state/states.html
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("state", new State());
        model.addAttribute("countries", countryRepo.findAll());
        return "state/states_form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model) {
        model.addAttribute("state", stateRepo.findById(id).orElse(new State()));
        model.addAttribute("countries", countryRepo.findAll());
        return "state/states_form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute State state) {
        Country country = countryRepo.findById(state.getCountry().getId())
                .orElseThrow(() -> new IllegalArgumentException("Country not found"));
        state.setCountry(country);
        stateRepo.save(state);
        return "redirect:/admin/states";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        stateRepo.deleteById(id);
        return "redirect:/admin/states";
    }
}