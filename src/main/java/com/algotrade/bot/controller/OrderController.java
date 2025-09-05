package com.algotrade.bot.controller;

import com.algotrade.bot.services.AngelOneService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
public class OrderController {

    private final AngelOneService angelOneService;

    public OrderController(AngelOneService angelOneService) {
        this.angelOneService = angelOneService;
    }

    @PostMapping("/placeOrder")
    public String placeOrder(HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            Map<String, Object> result = angelOneService.placeSampleOrder(session);
            redirectAttributes.addFlashAttribute("orderResult", result);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("orderError", e.getMessage());
        }
        // show result on place-order page
        return "redirect:/place-order";
    }
}
