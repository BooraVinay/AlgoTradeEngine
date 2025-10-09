package com.algotrade.bot.controller;

import com.algotrade.bot.model.TradingViewAlert;
import com.algotrade.bot.services.AlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/webhook")
public class TradingViewWebhookController {

    private final AlertService alertService;

    public TradingViewWebhookController(AlertService alertService) {
        this.alertService = alertService;
    }

    @PostMapping("/alert")
    public ResponseEntity<TradingViewAlert> receiveAlert(@RequestBody Map<String,Object> payload) {
        TradingViewAlert alert = alertService.createFromPayload(payload);
        return ResponseEntity.ok(alert);
    }
}
