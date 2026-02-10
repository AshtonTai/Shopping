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

    // GET /vouchers → show list
    @GetMapping
    public String listVouchers(Model model) {
        model.addAttribute("vouchers", voucherRepo.findAll());
        model.addAttribute("voucherTypes", VoucherType.values());
        return "vouchers/list";
    }

    // GET /vouchers/new → show form
    @GetMapping("/new")
    public String newVoucher(Model model) {
        model.addAttribute("voucher", new Voucher());
        model.addAttribute("voucherTypes", VoucherType.values());
        return "vouchers/form";
    }

    // POST /vouchers → save
    @PostMapping
    public String saveVoucher(@Valid Voucher voucher, BindingResult bindingResult,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Please correct the errors below");
            return "redirect:/vouchers/new";
        }

        if (voucherRepo.existsByCode(voucher.getCode())) {
            redirectAttributes.addFlashAttribute("error", "Voucher code already exists");
            return "redirect:/vouchers/new";
        }

        voucher.setUsageCount(0);
        voucherRepo.save(voucher);
        redirectAttributes.addFlashAttribute("message", "Voucher saved successfully!");
        return "redirect:/vouchers";
    }

    @PostMapping("/delete/{id}")
    public String deleteVoucher(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        voucherRepo.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "Voucher deleted successfully!");
        return "redirect:/vouchers";
    }
}
