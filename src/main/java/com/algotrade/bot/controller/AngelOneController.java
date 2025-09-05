package com.algotrade.bot.controller;

import com.algotrade.bot.services.AngelOneService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/angel")
public class AngelOneController {

    private final AngelOneService angelOneService;

    public AngelOneController(AngelOneService angelOneService) {
        this.angelOneService = angelOneService;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> profile(HttpSession session) {
        try {
            Map<String, Object> profile = angelOneService.getProfile(session);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Failed to get profile: " + e.getMessage());
        }
    }

    @PostMapping("/placeSampleOrder")
    public ResponseEntity<?> placeOrder(HttpSession session) {
        try {
            Map<String, Object> result = angelOneService.placeSampleOrder(session);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Order failed: " + e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        try {
            angelOneService.logout(session);
            return ResponseEntity.ok("Logged out");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Logout failed: " + e.getMessage());
        }
    }
}
