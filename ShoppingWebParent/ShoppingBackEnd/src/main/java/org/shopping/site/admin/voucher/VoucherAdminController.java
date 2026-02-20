package org.shopping.site.admin.voucher;

import org.shopping.entity.Voucher;
import org.shopping.entity.VoucherType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/vouchers")
@PreAuthorize("hasAnyAuthority('Admin', 'Editor')")
public class VoucherAdminController {

    @Autowired private VoucherService voucherService;
    @Autowired private VoucherRepository voucherRepo;

    @GetMapping
    public String listVouchers(Model model) {
        model.addAttribute("vouchers", voucherRepo.findAll());
        model.addAttribute("voucherTypes", VoucherType.values());
        return "vouchers/list";
    }

    @GetMapping("/new")
    public String newVoucher(Model model) {
        model.addAttribute("voucher", new Voucher());
        model.addAttribute("voucherTypes", VoucherType.values());
        return "vouchers/form";
    }

    @PostMapping
    public String saveVoucher(@Valid Voucher voucher, BindingResult bindingResult,
                              RedirectAttributes redirectAttributes, Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("voucherTypes", VoucherType.values());
            return "vouchers/form"; // Stay on form with errors (no redirect)
        }

        // Check duplicate code — but allow current voucher to keep its own code
        if (voucherRepo.existsByCodeAndIdNot(voucher.getCode(), voucher.getId())) {
            model.addAttribute("voucherTypes", VoucherType.values());
            model.addAttribute("error", "Voucher code already exists");
            return "vouchers/form";
        }

        voucher.setUsageCount(voucher.getUsageCount() == null ? 0 : voucher.getUsageCount());
        voucherRepo.save(voucher); // JPA handles insert or update automatically
        redirectAttributes.addFlashAttribute("message", "Voucher saved successfully!");
        return "redirect:/vouchers";
    }

    @GetMapping("/edit/{id}")
    public String editVoucher(@PathVariable Integer id, Model model) {
        Voucher voucher = voucherRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid voucher ID: " + id));

        model.addAttribute("voucher", voucher);
        model.addAttribute("voucherTypes", VoucherType.values());
        return "vouchers/form"; // Reuse the same form for create/edit
    }

    @PostMapping("/delete/{id}")
    public String deleteVoucher(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        voucherRepo.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "Voucher deleted successfully!");
        return "redirect:/vouchers";
    }
}
