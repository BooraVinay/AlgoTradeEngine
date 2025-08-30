package com.algotrade.bot.controller;

import com.algotrade.bot.services.AliceBlueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alice")
public class AliceBlueController {

    private final AliceBlueService aliceBlueService;

    public AliceBlueController(AliceBlueService aliceBlueService) {
        this.aliceBlueService = aliceBlueService;
    }

    @PostMapping("/placeOrder")
    public ResponseEntity<?> placeOrder() {
        return ResponseEntity.ok(aliceBlueService.placeNormalOrder());
    }


}
