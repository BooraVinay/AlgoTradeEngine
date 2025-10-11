package com.algotrade.bot.services;

import com.algotrade.bot.model.TradingViewAlert;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final Map<String, TradingViewAlert> alertStore = new ConcurrentHashMap<>();
    private final ObjectMapper mapper;
    private final File alertsFile = new File("alerts_data.json");

    public AlertService() {
        this.mapper = new ObjectMapper();
        // enable Java 8 Date/Time support
        this.mapper.registerModule(new JavaTimeModule());
        // prefer ISO strings rather than timestamps
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        loadFromFile();
    }

    private synchronized void loadFromFile() {
        if (!alertsFile.exists()) {
            log.info("alerts_data.json not found — starting with empty alert store.");
            return;
        }

        if (alertsFile.length() == 0L) {
            log.warn("alerts_data.json exists but is empty — starting with empty alert store.");
            return;
        }

        try {
            List<TradingViewAlert> list = mapper.readValue(alertsFile, new TypeReference<List<TradingViewAlert>>() {});
            alertStore.clear();
            if (list != null) {
                for (TradingViewAlert a : list) {
                    if (a != null && a.getId() != null) alertStore.put(a.getId(), a);
                }
            }
            log.info("Loaded {} alerts from {}", alertStore.size(), alertsFile.getAbsolutePath());
        } catch (JsonProcessingException jpe) {
            // Malformed JSON: move file aside and continue with empty store
            try {
                String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                File corrupt = new File(alertsFile.getParentFile(), alertsFile.getName() + ".corrupt." + ts);
                Files.move(alertsFile.toPath(), corrupt.toPath(), StandardCopyOption.REPLACE_EXISTING);
                log.error("alerts_data.json is malformed. Moved to: {}", corrupt.getAbsolutePath());
            } catch (IOException ex) {
                log.error("Failed to move corrupted alerts file: {}", ex.getMessage(), ex);
            }
            alertStore.clear();
        } catch (IOException e) {
            log.error("Error reading alerts file: {}", e.getMessage(), e);
            alertStore.clear();
        }
    }

    private synchronized void persistToFile() {
        try {
            List<TradingViewAlert> list = new ArrayList<>(alertStore.values());
            // sort by timestamp desc, nulls last
            list.sort(Comparator.comparing(TradingViewAlert::getTimestamp, Comparator.nullsLast(Comparator.naturalOrder())).reversed());

            // write to temp file in same directory and move atomically
            Path parent = alertsFile.toPath().toAbsolutePath().getParent();
            if (parent == null) parent = Path.of(".");
            Path tmp = Files.createTempFile(parent, "alerts_data", ".json.tmp");
            mapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), list);
            Files.move(tmp, alertsFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            log.debug("Persisted {} alerts to {}", list.size(), alertsFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to persist alerts to file: {}", e.getMessage(), e);
            // fallback: try direct write (non-atomic)
            try {
                mapper.writerWithDefaultPrettyPrinter().writeValue(alertsFile, new ArrayList<>(alertStore.values()));
            } catch (IOException ex) {
                log.error("Fallback write also failed: {}", ex.getMessage(), ex);
            }
        }
    }

    public synchronized TradingViewAlert createFromPayload(Map<String, Object> payload) {
        TradingViewAlert alert = new TradingViewAlert();
        alert.setId(UUID.randomUUID().toString());
        alert.setRawPayload(payload);
        alert.setTimestamp(LocalDateTime.now());

        alert.setTicker(Optional.ofNullable(payload.get("ticker")).map(Object::toString).orElse("UNKNOWN"));
        alert.setExchange(Optional.ofNullable(payload.get("exchange")).map(Object::toString).orElse("NSE"));
        alert.setInterval(Optional.ofNullable(payload.get("interval")).map(Object::toString).orElse("1"));
        alert.setTime(Optional.ofNullable(payload.get("time")).map(Object::toString).orElse(LocalDateTime.now().toString()));

        Object act = payload.get("action");
        if (act == null) act = payload.getOrDefault("strategy.order.action", null);
        alert.setAction(act != null ? act.toString().toUpperCase() : "BUY");

        int qty = 1;
        try {
            Object q = payload.get("qty");
            if (q != null) qty = Integer.parseInt(q.toString());
        } catch (Exception ignored) {}
        alert.setQuantity(Math.max(1, qty));

        if (payload.get("symboltoken") != null) alert.setSymboltoken(payload.get("symboltoken").toString());

        alert.setStatus("NEW");
        alertStore.put(alert.getId(), alert);
        persistToFile();
        log.info("[ALERT STORED] {} {}", alert.getTicker(), alert.getAction());
        return alert;
    }

    public List<TradingViewAlert> getTodaysAlerts() {
        List<TradingViewAlert> list = new ArrayList<>(alertStore.values());
        list.sort(Comparator.comparing(TradingViewAlert::getTimestamp, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        return list;
    }

    public Optional<TradingViewAlert> findById(String id) {
        return Optional.ofNullable(alertStore.get(id));
    }

    public synchronized void update(TradingViewAlert alert) {
        if (alert == null || alert.getId() == null) return;
        alertStore.put(alert.getId(), alert);
        persistToFile();
    }
}
