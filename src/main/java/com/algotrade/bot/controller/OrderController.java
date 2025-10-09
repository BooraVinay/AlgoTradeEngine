package com.algotrade.bot.controller;

import com.algotrade.bot.model.Scrip;
import com.algotrade.bot.services.AngelOneService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
public class OrderController {

    private final AngelOneService angelOneService;

    public OrderController(AngelOneService angelOneService) {
        this.angelOneService = angelOneService;
    }

    // --- your existing search and place-order endpoints (unchanged) ---
    @PostMapping("/scrip/search")
    public String searchScrip(
            HttpSession session,
            @RequestParam("tradingsymbol") String tradingsymbol,
            @RequestParam(value = "exchange", required = false, defaultValue = "NSE") String exchange,
            RedirectAttributes redirectAttributes
    ) {
        try {
            List<Map<String, Object>> results = angelOneService.searchScrip(session, tradingsymbol);
            redirectAttributes.addFlashAttribute("searchResults", results);
            redirectAttributes.addFlashAttribute("searchedSymbol", tradingsymbol);
            redirectAttributes.addFlashAttribute("searchedExchange", exchange);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("searchError", "Search failed: " + e.getMessage());
        }
        return "redirect:/place-order";
    }

    @PostMapping("/place-order")
    public String placeOrder(
            HttpSession session,
            RedirectAttributes redirectAttributes,
            @RequestParam("tradingsymbol") String tradingsymbol,
            @RequestParam("exchange") String exchange,
            @RequestParam("transactiontype") String transactiontype,
            @RequestParam("ordertype") String ordertype,
            @RequestParam("quantity") int quantity,
            @RequestParam(value = "price", required = false) Double price,
            @RequestParam("producttype") String producttype,
            @RequestParam("duration") String duration,
            @RequestParam(value = "symboltoken", required = false) String symboltoken
    ) {
        try {
            // Resolve symboltoken if not provided
            if (symboltoken == null || symboltoken.isBlank()) {
                try {
                    Scrip s = angelOneService.searchScrip(session, exchange, tradingsymbol);
                    if (s != null && s.getSymboltoken() != null) symboltoken = s.getSymboltoken();
                } catch (Exception ignored) { }
                if ((symboltoken == null || symboltoken.isBlank())) {
                    try {
                        var list = angelOneService.searchScrip(session, tradingsymbol);
                        if (list != null && !list.isEmpty()) {
                            Object tokenObj = list.get(0).get("symboltoken");
                            if (tokenObj != null) symboltoken = tokenObj.toString();
                        }
                    } catch (Exception ignored) { }
                }
            }

            if (symboltoken == null || symboltoken.isBlank()) {
                throw new RuntimeException("Symbol token not found. Please use Search Scrip or Validate.");
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("variety", "NORMAL");
            payload.put("tradingsymbol", tradingsymbol);
            payload.put("exchange", exchange);
            payload.put("transactiontype", transactiontype);
            payload.put("ordertype", ordertype);
            payload.put("quantity", String.valueOf(quantity));
            payload.put("producttype", producttype);
            payload.put("duration", duration);
            payload.put("price", price != null ? String.valueOf(price) : "0");
            payload.put("symboltoken", symboltoken);

            String clientCode = (String) session.getAttribute("clientCode");
            if (clientCode != null) payload.put("clientcode", clientCode);

            Map<String, Object> result = angelOneService.placeOrder(session, payload);

            String orderId = "N/A";
            if (result != null) {
                if (result.get("orderid") != null) orderId = result.get("orderid").toString();
                else if (result.get("uniqueorderid") != null) orderId = result.get("uniqueorderid").toString();
                else if (result.get("orderId") != null) orderId = result.get("orderId").toString();
            }

            redirectAttributes.addFlashAttribute("orderResult", result);
            redirectAttributes.addFlashAttribute("orderId", orderId);
            redirectAttributes.addFlashAttribute("statusMessage", "Order placed successfully!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("orderError", e.getMessage());
        }
        return "redirect:/trading-history";
    }

    @GetMapping("/api/scrip/search")
    @ResponseBody
    public ResponseEntity<?> apiSearchScrip(
            @RequestParam("ticker") String ticker,
            @RequestParam(value = "exchange", required = false, defaultValue = "NSE") String exchange,
            HttpSession session) {
        try {
            List<Map<String, Object>> results = angelOneService.searchScrip(session, ticker);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // --- CANCEL endpoint (accepts JSON or form) ---
    @PostMapping(value = "/orders/cancel", produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> cancelOrder(
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(value = "orderId", required = false) String orderIdParam,
            HttpSession session) {

        try {
            String orderId = null;
            if (body != null && (body.get("orderId") != null || body.get("orderid") != null)) {
                Object idObj = body.get("orderId") != null ? body.get("orderId") : body.get("orderid");
                orderId = idObj != null ? idObj.toString() : null;
            }
            if (orderId == null && orderIdParam != null) orderId = orderIdParam;

            if (orderId == null || orderId.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "orderId is required"));
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("variety", "NORMAL");
            payload.put("orderid", orderId);

            Map<String, Object> result = angelOneService.cancelOrder(session, payload);
            return ResponseEntity.ok(Map.of("success", true, "data", result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "error", e.getMessage() == null ? "Unknown error" : e.getMessage()));
        }
    }

    // --- MODIFY endpoint (AJAX existing) ---
    @PostMapping(value = "/orders/modify", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> modifyOrder(@RequestBody Map<String, Object> payload, HttpSession session) {
        try {
            // accept either "orderId" or "orderid"
            if (!payload.containsKey("orderid") && payload.containsKey("orderId")) {
                payload.put("orderid", payload.get("orderId"));
            }
            if (!payload.containsKey("variety")) payload.put("variety", "NORMAL");
            Map<String, Object> result = angelOneService.modifyOrder(session, payload);
            return ResponseEntity.ok(Map.of("success", true, "data", result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "error", e.getMessage() == null ? "Unknown error" : e.getMessage()));
        }
    }

    // --- SHOW Modify Page (GET) ---
    @GetMapping("/orders/modify")
    public String showModifyPage(
            @RequestParam(value = "orderId", required = false) String orderId,
            @RequestParam(value = "uniqueOrderId", required = false) String uniqueOrderId,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            Map<String, Object> orderData = null;

            // Try uniqueOrderId via getOrderDetails
            if (uniqueOrderId != null && !uniqueOrderId.isBlank()) {
                Map<String, Object> resp = angelOneService.getOrderDetails(session, uniqueOrderId);
                if (resp != null && Boolean.TRUE.equals(resp.get("status"))) {
                    Object d = resp.get("data");
                    if (d instanceof Map) orderData = (Map<String, Object>) d;
                }
            }

            // Fallback: search orderbook by orderId
            if (orderData == null && orderId != null && !orderId.isBlank()) {
                Map<String, Object> ob = angelOneService.getOrderBook(session);
                if (ob != null && Boolean.TRUE.equals(ob.get("status"))) {
                    Object raw = ob.get("data");
                    if (raw instanceof List) {
                        List<Map<String, Object>> data = (List<Map<String, Object>>) raw;
                        for (Map<String, Object> item : data) {
                            Object oid = item.get("orderid");
                            if (oid != null && orderId.equals(String.valueOf(oid))) {
                                orderData = item;
                                break;
                            }
                        }
                    }
                }
            }

            if (orderData == null) {
                redirectAttributes.addFlashAttribute("orderError", "Unable to find order details to modify.");
                return "redirect:/trading-history";
            }

            // Pass orderData to model with convenient attributes
            model.addAttribute("order", orderData);
            model.addAttribute("orderId", orderData.getOrDefault("orderid", ""));
            model.addAttribute("uniqueOrderId", orderData.getOrDefault("uniqueorderid", ""));
            model.addAttribute("tradingsymbol", orderData.getOrDefault("tradingsymbol", ""));
            model.addAttribute("exchange", orderData.getOrDefault("exchange", ""));
            model.addAttribute("ordertype", orderData.getOrDefault("ordertype", ""));
            model.addAttribute("producttype", orderData.getOrDefault("producttype", ""));
            model.addAttribute("duration", orderData.getOrDefault("duration", ""));
            model.addAttribute("price", orderData.getOrDefault("price", ""));
            model.addAttribute("quantity", orderData.getOrDefault("quantity", ""));
            model.addAttribute("symboltoken", orderData.getOrDefault("symboltoken", ""));

            // show page
            return "modify-order";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("orderError", "Failed to load order details: " + e.getMessage());
            return "redirect:/trading-history";
        }
    }

    // --- Submit modify form (page POST) ---
    @PostMapping("/orders/modify-form")
    public String submitModifyForm(
            HttpSession session,
            RedirectAttributes redirectAttributes,
            @RequestParam("orderId") String orderId,
            @RequestParam(value = "price", required = false) String price,
            @RequestParam(value = "quantity", required = false) String quantity,
            @RequestParam(value = "ordertype", required = false) String ordertype,
            @RequestParam(value = "producttype", required = false) String producttype,
            @RequestParam(value = "duration", required = false) String duration,
            @RequestParam(value = "tradingsymbol", required = false) String tradingsymbol,
            @RequestParam(value = "exchange", required = false) String exchange,
            @RequestParam(value = "symboltoken", required = false) String symboltoken
    ) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("variety", "NORMAL");
            payload.put("orderid", orderId);

            // include order fields if provided
            if (ordertype != null && !ordertype.isBlank()) payload.put("ordertype", ordertype);
            if (producttype != null && !producttype.isBlank()) payload.put("producttype", producttype);
            if (duration != null && !duration.isBlank()) payload.put("duration", duration);
            if (price != null && !price.isBlank()) payload.put("price", price);
            if (quantity != null && !quantity.isBlank()) payload.put("quantity", quantity);

            // include optional fields that some APIs expect for modify
            if (tradingsymbol != null && !tradingsymbol.isBlank()) payload.put("tradingsymbol", tradingsymbol);
            if (exchange != null && !exchange.isBlank()) payload.put("exchange", exchange);
            if (symboltoken != null && !symboltoken.isBlank()) payload.put("symboltoken", symboltoken);

            Map<String, Object> result = angelOneService.modifyOrder(session, payload);

            redirectAttributes.addFlashAttribute("modifySuccess", "Order modified successfully.");
            redirectAttributes.addFlashAttribute("modifyResult", result);

            // redirect back to modify page to show updated data
            return "redirect:/orders/modify?orderId=" + orderId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("modifyError", "Modify failed: " + e.getMessage());
            return "redirect:/orders/modify?orderId=" + orderId;
        }
    }
}
