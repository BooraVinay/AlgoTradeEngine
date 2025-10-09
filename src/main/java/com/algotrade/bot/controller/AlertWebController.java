package com.algotrade.bot.controller;

import com.algotrade.bot.model.Scrip;
import com.algotrade.bot.model.TradingViewAlert;
import com.algotrade.bot.services.AlertService;
import com.algotrade.bot.services.AngelOneService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/alerts")
public class AlertWebController {
    private final AlertService alertService;
    private final AngelOneService angelOneService;

    public AlertWebController(AlertService alertService, AngelOneService angelOneService) {
        this.alertService = alertService;
        this.angelOneService = angelOneService;
    }

    @GetMapping("/today")
    public String listAlerts(Model model) {
        List<TradingViewAlert> alerts = alertService.getTodaysAlerts();
        model.addAttribute("alerts", alerts);
        return "alerts"; // templates/alerts.html
    }

    @PostMapping("/{id}/accept")
    public String acceptAlert(@PathVariable String id, HttpSession session) {
        Optional<TradingViewAlert> optional = alertService.findById(id);
        if (optional.isEmpty()) return "redirect:/alerts/today";

        TradingViewAlert alert = optional.get();
        try {
            // Only equity as per your requirement, NSE default
            Scrip scrip = angelOneService.searchScrip(session, "NSE", alert.getTicker());
            if (scrip == null) throw new RuntimeException("Instrument not found for " + alert.getTicker());

            Map<String, Object> orderPayload = Map.of(
                    "variety", "NORMAL",
                    "tradingsymbol", scrip.getTradingsymbol(),
                    "symboltoken", scrip.getSymboltoken(),
                    "transactiontype", alert.getAction(),
                    "exchange", "NSE",
                    "ordertype", "MARKET",
                    "producttype", "INTRADAY",
                    "duration", "DAY",
                    "price", "0",
                    "quantity", String.valueOf(alert.getQuantity())
            );

            Map<String,Object> result = angelOneService.placeOrder(session, orderPayload);
            alert.setStatus("ACCEPTED");
            alert.setOrderResult(result);
            alertService.update(alert);
        } catch (Exception ex) {
            alert.setStatus("FAILED");
            alert.setErrorMessage(ex.getMessage());
            alertService.update(alert);
        }
        return "redirect:/alerts/today";
    }

    @PostMapping("/{id}/reject")
    public String rejectAlert(@PathVariable String id) {
        Optional<TradingViewAlert> optional = alertService.findById(id);
        optional.ifPresent(a -> {
            a.setStatus("REJECTED");
            alertService.update(a);
        });
        return "redirect:/alerts/today";
    }


}
