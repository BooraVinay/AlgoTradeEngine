package com.algotrade.bot.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Service
public class AliceBlueService {
    private static final Logger log = LoggerFactory.getLogger(AliceBlueService.class);

    @Value("${aliceblue.base-url}")
    private String baseUrl;

    @Value("${aliceblue.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private String sessionId; // cached session

    // Step 1: Get Encryption Key
    public String getEncryptionKey(String userId) {
        String url = baseUrl + "/customer/getAPIEncpkey";
        Map<String, String> req = Map.of("userId", userId);

        HttpEntity<Map<String, String>> entity =
                new HttpEntity<>(req, getJsonHeaders());

        ResponseEntity<Map> response =
                restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

        if ("Ok".equals(response.getBody().get("stat"))) {
            return (String) response.getBody().get("encKey");
        }
        throw new RuntimeException("Failed to get encKey: " + response.getBody().get("emsg"));
    }

    // Step 2: Generate SHA-256
    private String generateSHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 error", e);
        }
    }

    // Step 3: Generate Session ID
    public String generateSessionId(String userId) {
        String encKey = getEncryptionKey(userId);
        String userData = generateSHA256(userId + apiKey + encKey);

        String url = baseUrl + "/customer/getUserSID";
        Map<String, String> req = Map.of("userId", userId, "userData", userData);

        HttpEntity<Map<String, String>> entity =
                new HttpEntity<>(req, getJsonHeaders());

        ResponseEntity<Map> response =
                restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

        if ("Ok".equals(response.getBody().get("stat"))) {
            sessionId = (String) response.getBody().get("sessionID");
            log.info("Generated broker session {}", sessionId);
            return sessionId;
        }
        throw new RuntimeException("Failed to get sessionID: " + response.getBody().get("emsg"));
    }

    // Step 4: Return cached session
    public String getSessionId(String userId) {
        return (sessionId == null) ? generateSessionId(userId) : sessionId;
    }

    // Reset cached session
    public void resetSession() {
        this.sessionId = null;
    }

    // Place order using cached session
    public List<Map<String, Object>> placeNormalOrder(String userId) {
        String url = baseUrl + "/placeOrder/executePlaceOrder";

        Map<String, Object> order = new HashMap<>();
        order.put("complexty", "regular");
        order.put("discqty", "0");
        order.put("exch", "NSE");
        order.put("pCode", "MIS");
        order.put("prctyp", "L");
        order.put("price", "3550.00");
        order.put("qty", 1);
        order.put("ret", "DAY");
        order.put("symbol_id", "212");
        order.put("trading_symbol", "ASHOKLEY-EQ");
        order.put("transtype", "BUY");
        order.put("trigPrice", "0.00");
        order.put("orderTag", "order1");
        order.put("deviceNumber", "device123");

        List<Map<String, Object>> payload = List.of(order);

        HttpHeaders headers = getJsonHeaders();
        headers.setBearerAuth(getSessionId(userId));

        HttpEntity<List<Map<String, Object>>> entity = new HttpEntity<>(payload, headers);

        ResponseEntity<List> response =
                restTemplate.exchange(url, HttpMethod.POST, entity, List.class);

        return response.getBody();
    }

    private HttpHeaders getJsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
