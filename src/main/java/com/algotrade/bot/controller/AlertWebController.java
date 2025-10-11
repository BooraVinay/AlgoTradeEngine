package com.algotrade.bot.controller;

import com.algotrade.bot.model.Scrip;
import com.algotrade.bot.model.TradingViewAlert;
import com.algotrade.bot.services.AlertService;
import com.algotrade.bot.services.AngelOneService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Controller
@RequestMapping("/alerts")
public class AlertWebController {

    private final AlertService alertService;
    private final AngelOneService angelOneService;

    private final ObjectMapper mapper = new ObjectMapper();
    private final File stateFile = new File("alerts_state.json");

    // persisted flag
    private boolean stopAlerts = false;

    public AlertWebController(AlertService alertService, AngelOneService angelOneService) {
        this.alertService = alertService;
        this.angelOneService = angelOneService;
        loadStopAlertsState();
    }

    private void loadStopAlertsState() {
        try {
            if (stateFile.exists()) {
                Map<String, Boolean> state = mapper.readValue(stateFile, new TypeReference<>() {});
                stopAlerts = state.getOrDefault("stopAlerts", false);
            }
        } catch (IOException e) {
            e.printStackTrace();
            stopAlerts = false;
        }
    }

    private void saveStopAlertsState() {
        try {
            mapper.writeValue(stateFile, Map.of("stopAlerts", stopAlerts));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/today")
    public String listAlerts(Model model, HttpSession session) {
        List<TradingViewAlert> alerts = alertService.getTodaysAlerts();
        model.addAttribute("alerts", alerts);
        model.addAttribute("stopAlerts", stopAlerts);

        @SuppressWarnings("unchecked")
        Set<String> seen = (Set<String>) session.getAttribute("seenAlerts");
        if (seen == null) {
            seen = new HashSet<>();
            for (TradingViewAlert a : alerts) if (a.getId() != null) seen.add(a.getId());
            session.setAttribute("seenAlerts", seen);
        }
        return "alerts";
    }

    @PostMapping("/{id}/accept")
    public String acceptAlert(@PathVariable String id, HttpSession session, RedirectAttributes redirectAttributes) {
        Optional<TradingViewAlert> opt = alertService.findById(id);
        if (opt.isEmpty()) {
            redirectAttributes.addFlashAttribute("orderError", "Alert not found");
            return "redirect:/alerts/today";
        }
        TradingViewAlert alert = opt.get();

        try {
            String jwt = (String) session.getAttribute("jwtToken");
            String clientCode = (String) session.getAttribute("clientCode");
            if (jwt == null || clientCode == null) throw new RuntimeException("Not logged in.");

            String symboltoken = alert.getSymboltoken();
            String tradingsymbol = Optional.ofNullable(alert.getTicker()).orElse("").toUpperCase();

            Map<String, String> manualMap = Map.of("RELIANCE", "RELIANCE-EQ");
            if (manualMap.containsKey(tradingsymbol)) tradingsymbol = manualMap.get(tradingsymbol);

            if (symboltoken == null || symboltoken.isBlank()) {
                try {
                    Scrip s = angelOneService.searchScrip(session, "NSE", tradingsymbol);
                    if (s != null) {
                        symboltoken = s.getSymboltoken();
                        tradingsymbol = s.getTradingsymbol();
                    }
                } catch (Exception ex) {
                    try {
                        List<Map<String, Object>> results = angelOneService.searchScrip(session, tradingsymbol);
                        if (results != null && !results.isEmpty() && results.get(0).get("symboltoken") != null) {
                            symboltoken = results.get(0).get("symboltoken").toString();
                            if (results.get(0).get("tradingsymbol") != null)
                                tradingsymbol = results.get(0).get("tradingsymbol").toString();
                        }
                    } catch (Exception ignored) {}
                }
            }

            if (symboltoken == null || symboltoken.isBlank()) {
                String msg = "Symbol not found for ticker: " + alert.getTicker();
                alert.setStatus("FAILED");
                alert.setErrorMessage(msg);
                alertService.update(alert);
                redirectAttributes.addFlashAttribute("orderError", msg);
                markSeenInSession(session, alert.getId());
                return "redirect:/alerts/today";
            }

            Map<String, Object> order = new HashMap<>();
            order.put("variety", "NORMAL");
            order.put("tradingsymbol", tradingsymbol);
            order.put("symboltoken", symboltoken);
            order.put("transactiontype", Optional.ofNullable(alert.getAction()).orElse("BUY").toUpperCase());
            order.put("exchange", Optional.ofNullable(alert.getExchange()).orElse("NSE"));
            order.put("ordertype", "MARKET");
            order.put("producttype", "INTRADAY");
            order.put("duration", "DAY");
            order.put("price", "0");
            order.put("quantity", String.valueOf(Math.max(alert.getQuantity(), 1)));
            order.put("clientcode", clientCode);

            Map<String, Object> result = angelOneService.placeOrder(session, order);

            alert.setStatus("ACCEPTED");
            alert.setOrderResult(result);
            alertService.update(alert);

            markSeenInSession(session, alert.getId());
            redirectAttributes.addFlashAttribute("orderResult", "Order accepted for " + alert.getTicker());

        } catch (Exception e) {
            alert.setStatus("FAILED");
            alert.setErrorMessage(e.getMessage());
            alertService.update(alert);
            markSeenInSession(session, alert.getId());
            redirectAttributes.addFlashAttribute("orderError", e.getMessage());
        }

        return "redirect:/alerts/today";
    }

    @PostMapping("/{id}/reject")
    public String rejectAlert(@PathVariable String id, HttpSession session, RedirectAttributes redirectAttributes) {
        Optional<TradingViewAlert> opt = alertService.findById(id);
        opt.ifPresent(alert -> {
            alert.setStatus("REJECTED");
            alertService.update(alert);
            markSeenInSession(session, alert.getId());
            redirectAttributes.addFlashAttribute("message", "Alert rejected for " + alert.getTicker());
        });
        return "redirect:/alerts/today";
    }

    private void markSeenInSession(HttpSession session, String alertId) {
        if (alertId == null) return;
        @SuppressWarnings("unchecked")
        Set<String> seen = (Set<String>) session.getAttribute("seenAlerts");
        if (seen == null) {
            seen = new HashSet<>();
            session.setAttribute("seenAlerts", seen);
        }
        seen.add(alertId);
    }

    @PostMapping("/ajax/toggle")
    @ResponseBody
    public Map<String, Object> toggleAlertsAjax() {
        stopAlerts = !stopAlerts;
        saveStopAlertsState();
        return Map.of("stopAlerts", stopAlerts);
    }

    @GetMapping("/new")
    @ResponseBody
    public List<TradingViewAlert> getNewAlerts(HttpSession session) {
        if (stopAlerts) return Collections.emptyList();

        List<TradingViewAlert> alerts = alertService.getTodaysAlerts();
        @SuppressWarnings("unchecked")
        Set<String> seen = (Set<String>) session.getAttribute("seenAlerts");
        if (seen == null) {
            seen = new HashSet<>();
            for (TradingViewAlert a : alerts) if (a.getId() != null) seen.add(a.getId());
            session.setAttribute("seenAlerts", seen);
            return Collections.emptyList();
        }

        List<TradingViewAlert> newAlerts = new ArrayList<>();
        for (TradingViewAlert a : alerts) {
            if (a.getId() == null) continue;
            if (!seen.contains(a.getId())) {
                newAlerts.add(a);
                seen.add(a.getId());
            }
        }
        return newAlerts;
    }
}
