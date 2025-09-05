package com.algotrade.bot.controller;

import com.algotrade.bot.services.AliceBlueService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/*@RestController
@RequestMapping("/api/alice")
public class AliceBlueController {

    private final AliceBlueService aliceBlueService;

    public AliceBlueController(AliceBlueService aliceBlueService) {
        this.aliceBlueService = aliceBlueService;
    }

    @PostMapping("/placeOrder")
    public ResponseEntity<?> placeOrder(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        String brokerSession = (String) session.getAttribute("brokerSession");

        if (brokerSession == null) {
            return ResponseEntity.status(401).body("Not logged in");
        }

        try {
            List<Map<String, Object>> resp = aliceBlueService.placeNormalOrder(userId);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            aliceBlueService.resetSession();
            session.invalidate();
            return ResponseEntity.status(400).body("Order failed: " + e.getMessage());
        }
    }
}*/
