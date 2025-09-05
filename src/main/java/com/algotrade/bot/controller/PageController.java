package com.algotrade.bot.controller;

import com.algotrade.bot.services.AngelOneService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@Controller
public class PageController {

    private final AngelOneService angelOneService;

    public PageController(AngelOneService angelOneService) {
        this.angelOneService = angelOneService;
    }

    // Landing: redirect to login if not authenticated; otherwise show dashboard
    @GetMapping({"/", "/dashboard"})
    public String dashboard(HttpSession session, Model model) {
        String clientCode = (String) session.getAttribute("clientCode");
        if (clientCode == null) return "redirect:/auth/";         // <-- updated to /auth/

        @SuppressWarnings("unchecked")
        Map<String, Object> profile = (Map<String, Object>) session.getAttribute("profile");
        if (profile == null) {
            try {
                profile = angelOneService.getProfile(session);
                session.setAttribute("profile", profile);
            } catch (Exception e) {
                // keep page rendering but show an error message
                model.addAttribute("errorMessage", "Failed to fetch profile: " + e.getMessage());
            }
        }

        model.addAttribute("clientCode", clientCode);
        model.addAttribute("profile", profile);
        return "dashboard"; // templates/dashboard.html
    }

    @GetMapping("/account-info")
    public String accountInfo(HttpSession session, Model model) {
        String clientCode = (String) session.getAttribute("clientCode");
        if (clientCode == null) return "redirect:/auth/";         // <-- updated to /auth/

        try {
            Map<String, Object> profile = angelOneService.getProfile(session);
            model.addAttribute("profile", profile);

            // optionally fetch RMS/margins for account-info view (uncomment if you want)
            // Map<String,Object> rms = angelOneService.getRms(session);
            // model.addAttribute("rms", rms);

        } catch (Exception e) {
            model.addAttribute("errorMessage", "Failed to fetch profile: " + e.getMessage());
        }

        model.addAttribute("clientCode", clientCode);
        return "account-info"; // templates/account-info.html
    }

    @GetMapping("/place-order")
    public String placeOrderPage(HttpSession session, Model model) {
        String clientCode = (String) session.getAttribute("clientCode");
        if (clientCode == null) return "redirect:/auth/";         // <-- updated to /auth/

        model.addAttribute("clientCode", clientCode);
        // flash attributes like orderResult / orderError will be available automatically after redirects
        return "place-order"; // templates/place-order.html
    }

    @GetMapping("/trading-history")
    public String tradingHistory(HttpSession session, Model model) {
        String clientCode = (String) session.getAttribute("clientCode");
        if (clientCode == null) return "redirect:/auth/";         // <-- updated to /auth/

        try {
            Map<String, Object> orderBook = angelOneService.getOrderBook(session);
            model.addAttribute("orderBook", orderBook);
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Failed to fetch order book: " + e.getMessage());
        }

        model.addAttribute("clientCode", clientCode);
        return "trading-history"; // templates/trading-history.html
    }

    @GetMapping("/trade-book")
    public String tradeBook(HttpSession session, Model model) {
        String clientCode = (String) session.getAttribute("clientCode");
        if (clientCode == null) return "redirect:/auth/";         // <-- updated to /auth/

        try {
            Map<String, Object> tradeBook = angelOneService.getTradeBook(session);
            model.addAttribute("tradeBook", tradeBook);
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Failed to fetch trade book: " + e.getMessage());
        }

        model.addAttribute("clientCode", clientCode);
        return "trade-book"; // templates/trade-book.html
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
        return "portfolio"; // create templates/portfolio.html
    }
}
