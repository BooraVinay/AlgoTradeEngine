package com.algotrade.bot.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AliceBlueService {
    private static final Logger log = LoggerFactory.getLogger(AliceBlueService.class);
    @Value("${aliceblue.base-url}")
    private String baseUrl;

    @Value("${aliceblue.user-id}")
    private String userId;

    @Value("${aliceblue.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private String sessionId; // cached session

    // Step 1: Get Encryption Key
    public String getEncryptionKey() {
        String url = baseUrl + "/customer/getAPIEncpkey";

        Map<String, String> request = new HashMap<>();
        request.put("userId", userId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

        if ("Ok".equals(response.getBody().get("stat"))) {
            return (String) response.getBody().get("encKey");
        } else {
            throw new RuntimeException("Failed to get encryption key: " + response.getBody().get("emsg"));
        }
    }

    // Step 2: Generate SHA-256
    private String generateSHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate SHA-256 hash", e);
        }
    }

    // Step 3: Get Session ID
    public String generateSessionId() {
        String encKey = getEncryptionKey();
        String combinedData = userId + apiKey + encKey;
        String userData = generateSHA256(combinedData);

        String url = baseUrl + "/customer/getUserSID";

        Map<String, String> request = new HashMap<>();
        request.put("userId", userId);
        request.put("userData", userData);



        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

        if ("Ok".equals(response.getBody().get("stat"))) {
            sessionId = (String) response.getBody().get("sessionID");
            log.info("Generated new session ID: {}", sessionId);
            return sessionId;
        } else {
            throw new RuntimeException("Failed to get session ID: " + response.getBody().get("emsg"));
        }
    }

    // Step 4: Get session ID (cached)
    public String getSessionId() {
        if (sessionId == null) {
            log.info("No cached session, generating new one...");
            return generateSessionId();
        }
        log.info("Using cached session ID: {}", sessionId);
        return sessionId;
    }

    // Reset session if needed
    public void resetSession() {
        this.sessionId = null;
    }


    public List<Map<String, Object>> placeNormalOrder() {
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
        order.put("deviceNumber", "sdagds345324dsfgfvasdqwr4");

        List<Map<String, Object>> payload = new ArrayList<>();
        payload.add(order);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(getSessionId());

        HttpEntity<List<Map<String, Object>>> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.POST, entity, List.class);
            return response.getBody();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // Catch HTTP errors (like 401, 400, etc.)
            log.error("Error placing order: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to place order: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            // Catch any other errors
            log.error("Unexpected error placing order", e);
            throw new RuntimeException("Unexpected error placing order: " + e.getMessage());
        }
    }

}
