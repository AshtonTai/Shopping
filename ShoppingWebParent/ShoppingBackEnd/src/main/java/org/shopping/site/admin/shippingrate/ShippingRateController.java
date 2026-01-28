package org.shopping.site.admin.shippingrate;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ShippingRateController {

    @Autowired
    private ShippingRateRepository shippingRateRepo;

    @GetMapping("/shipping-rates")
    public String shippingRates(Model model) {
        model.addAttribute("rates", shippingRateRepo.findAll());
        return "shipping-rates"; // matches your shipping_rate.html
    }
}
