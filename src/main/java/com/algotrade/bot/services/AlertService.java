package com.algotrade.bot.services;

import com.algotrade.bot.model.TradingViewAlert;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AlertService {
    private final Map<String, TradingViewAlert> alertStore = new ConcurrentHashMap<>();

    public TradingViewAlert createFromPayload(Map<String, Object> payload) {
        TradingViewAlert alert = new TradingViewAlert();
        alert.setId(UUID.randomUUID().toString());
        alert.setTicker((String) payload.get("ticker"));
        alert.setExchange((String) payload.getOrDefault("exchange", "NSE"));
        alert.setInterval((String) payload.get("interval"));
        alert.setTime((String) payload.get("time"));
        // action may come in different fields: check payload keys
        String action = null;
        if (payload.get("action") != null) action = payload.get("action").toString();
        else if (payload.get("strategy.order.action") != null) action = payload.get("strategy.order.action").toString();
        if (action != null) alert.setAction(action.toUpperCase());
        else alert.setAction("BUY"); // default

        alert.setQuantity(1);
        alert.setTimestamp(LocalDateTime.now());
        alertStore.put(alert.getId(), alert);
        return alert;
    }

    public List<TradingViewAlert> getTodaysAlerts() {
        return new ArrayList<>(alertStore.values());
    }

    public Optional<TradingViewAlert> findById(String id) {
        return Optional.ofNullable(alertStore.get(id));
    }

    public void update(TradingViewAlert alert) {
        alertStore.put(alert.getId(), alert);
    }
}
