package com.algotrade.bot.controller;

import com.algotrade.bot.model.Scrip;
import com.algotrade.bot.model.TradingViewAlert;
import com.algotrade.bot.services.AlertService;
import com.algotrade.bot.services.AngelOneService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@Controller
public class PageController {

    private final AngelOneService angelOneService;
    private final AlertService alertService;

    public PageController(AngelOneService angelOneService, AlertService alertService) {
        this.angelOneService = angelOneService;
        this.alertService = alertService;
    }

    // Landing/dashboard
    @GetMapping({"/", "/dashboard"})
    public String dashboard(HttpSession session, Model model) {
        String clientCode = (String) session.getAttribute("clientCode");
        if (clientCode == null) return "redirect:/auth/";

        @SuppressWarnings("unchecked")
        Map<String, Object> profile = (Map<String, Object>) session.getAttribute("profile");
        if (profile == null) {
            try {
                profile = angelOneService.getProfile(session);
                session.setAttribute("profile", profile);
            } catch (Exception e) {
                model.addAttribute("errorMessage", "Failed to fetch profile: " + e.getMessage());
            }
        }

        model.addAttribute("clientCode", clientCode);
        model.addAttribute("profile", profile);
        return "dashboard";
    }

    @GetMapping("/account-info")
    public String accountInfo(HttpSession session, Model model) {
        String clientCode = (String) session.getAttribute("clientCode");
        if (clientCode == null) return "redirect:/auth/";

        try {
            Map<String, Object> profile = angelOneService.getProfile(session);
            model.addAttribute("profile", profile);
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Failed to fetch profile: " + e.getMessage());
        }

        model.addAttribute("clientCode", clientCode);
        return "account-info";
    }


    @GetMapping("/place-order")
    public String placeOrderPage(
            HttpSession session,
            Model model,
            @RequestParam(value = "tradingsymbol", required = false) String prefTradingsymbol,
            @RequestParam(value = "symboltoken", required = false) String prefSymboltoken
    ) {
        String clientCode = (String) session.getAttribute("clientCode");
        if (clientCode == null) return "redirect:/auth/";

        model.addAttribute("clientCode", clientCode);

        // provide prefill values (if any) so Thymeleaf can fill the form
        if (prefTradingsymbol != null) model.addAttribute("prefillSymbol", prefTradingsymbol);
        if (prefSymboltoken != null) model.addAttribute("prefillToken", prefSymboltoken);

        return "place-order";
    }
    @GetMapping("/trading-history")
    public String tradingHistory(HttpSession session, Model model) {
        String clientCode = (String) session.getAttribute("clientCode");
        if (clientCode == null) return "redirect:/auth/";

        try {
            Map<String, Object> orderBook = angelOneService.getOrderBook(session);
            model.addAttribute("orderBook", orderBook);
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Failed to fetch order book: " + e.getMessage());
        }

        model.addAttribute("clientCode", clientCode);
        return "trading-history";
    }

    @GetMapping("/trade-book")
    public String tradeBook(HttpSession session, Model model) {
        String clientCode = (String) session.getAttribute("clientCode");
        if (clientCode == null) return "redirect:/auth/";

        try {
            Map<String, Object> tradeBook = angelOneService.getTradeBook(session);
            model.addAttribute("tradeBook", tradeBook);
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Failed to fetch trade book: " + e.getMessage());
        }

        model.addAttribute("clientCode", clientCode);
        return "trade-book";
    }

    @GetMapping("/portfolio")
    public String portfolio(HttpSession session, Model model) {
        String clientCode = (String) session.getAttribute("clientCode");
        if (clientCode == null) return "redirect:/auth/";

        try {
            Map<String, Object> holdings = angelOneService.getAllHoldings(session);
            model.addAttribute("holdings", holdings);
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Failed to fetch portfolio: " + e.getMessage());
        }

        model.addAttribute("clientCode", clientCode);
        return "portfolio";
    }

    // Scrip search UI (GET shows form & optionally results)
    @GetMapping("/scrip/search")
    public String scripSearchPage(@RequestParam(required = false) String ticker,
                                  HttpSession session,
                                  Model model) {
        if (ticker != null && !ticker.isBlank()) {
            try {
                Scrip s = angelOneService.searchScrip(session, "NSE", ticker);
                model.addAttribute("results", s == null ? List.of() : List.of(s));
            } catch (Exception e) {
                model.addAttribute("error", "Search error: " + e.getMessage());
            }
        }
        return "searchscrip";
    }

}
