package com.algotrade.bot.services;

import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import com.algotrade.bot.model.*;
import java.util.*;


@Service
public class AngelOneService {
    private static final Logger log = LoggerFactory.getLogger(AngelOneService.class);

    private static final String BASE_URL = "https://apiconnect.angelone.in/rest";

    @Value("${angel.apikey}")
    private String apiKey;

    @Value("${angel.password}")
    private String password; // PIN configured in properties

    @Value("${angel.clientLocalIp:127.0.0.1}")
    private String clientLocalIp;

    @Value("${angel.clientPublicIp:127.0.0.1}")
    private String clientPublicIp;

    @Value("${angel.macAddress:00:00:00:00:00:00}")
    private String macAddress;


    private final RestTemplate restTemplate = new RestTemplate();

    // Endpoints
    private static final String LOGIN_URL = BASE_URL + "/auth/angelbroking/user/v1/loginByPassword";
    private static final String GENERATE_TOKENS_URL = BASE_URL + "/auth/angelbroking/jwt/v1/generateTokens";
    private static final String GET_PROFILE_URL = BASE_URL + "/secure/angelbroking/user/v1/getProfile";
    private static final String GET_RMS_URL = BASE_URL + "/secure/angelbroking/user/v1/getRMS";
    private static final String PLACE_ORDER_URL = BASE_URL + "/secure/angelbroking/order/v1/placeOrder";
    private static final String MODIFY_ORDER_URL = BASE_URL + "/secure/angelbroking/order/v1/modifyOrder";
    private static final String CANCEL_ORDER_URL = BASE_URL + "/secure/angelbroking/order/v1/cancelOrder";
    private static final String GET_ORDER_BOOK_URL = BASE_URL + "/secure/angelbroking/order/v1/getOrderBook";
    private static final String GET_TRADE_BOOK_URL = BASE_URL + "/secure/angelbroking/order/v1/getTradeBook";
    private static final String GET_LTP_URL = BASE_URL + "/secure/angelbroking/order/v1/getLtpData";
    private static final String GET_ORDER_DETAILS_URL_TEMPLATE = BASE_URL + "/secure/angelbroking/order/v1/details/%s";
    private static final String LOGOUT_URL = BASE_URL + "/secure/angelbroking/user/v1/logout";
    private static final String GET_ALL_HOLDING_URL = BASE_URL + "/secure/angelbroking/portfolio/v1/getAllHolding";


    private static final String SEARCH_SCRIP_URL = BASE_URL + "/secure/angelbroking/order/v1/searchScrip";
    private static final String INTRADAY_NSE_URL = BASE_URL + "/secure/angelbroking/marketData/v1/nseIntraday";
    // -----------------------
    // AUTH
    // -----------------------

    /**
     * Login using clientCode (client code = userId) and totp.
     * Stores jwtToken, refreshToken, feedToken, clientCode into session.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> login(String clientCode, String totp, HttpSession session) {
        Map<String, String> body = new HashMap<>();
        body.put("clientcode", clientCode);
        body.put("password", password);
        body.put("totp", totp);
        body.put("state", "STATE");

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, baseHeaders());
        try {
            ResponseEntity<Map> respEntity = restTemplate.exchange(LOGIN_URL, HttpMethod.POST, request, Map.class);
            Map<String, Object> resp = respEntity.getBody();
            Boolean ok = resp != null ? (Boolean) resp.get("status") : Boolean.FALSE;
            if (Boolean.TRUE.equals(ok)) {
                Map<String, Object> data = (Map<String, Object>) resp.get("data");
                if (data == null) throw new RuntimeException("Login succeeded but no token data returned.");

                session.setAttribute("jwtToken", data.get("jwtToken"));
                session.setAttribute("refreshToken", data.get("refreshToken"));
                session.setAttribute("feedToken", data.get("feedToken"));
                session.setAttribute("clientCode", clientCode);

                log.info("login: success for clientCode={}", clientCode);
                return data;
            } else {
                throw new RuntimeException("Login failed: " + (resp != null ? resp.get("message") : "unknown"));
            }
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Login HTTP error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        }
    }

    /**
     * Refresh JWT using refreshToken stored in session. Updates session tokens and returns token data.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> refreshTokens(HttpSession session) {
        String refreshToken = (String) session.getAttribute("refreshToken");
        if (refreshToken == null) throw new RuntimeException("No refresh token in session.");

        Map<String, String> body = new HashMap<>();
        body.put("refreshToken", refreshToken);

        HttpHeaders headers = baseHeaders();
        String jwt = (String) session.getAttribute("jwtToken");
        if (jwt != null) headers.set("Authorization", "Bearer " + jwt);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> respEntity = restTemplate.exchange(GENERATE_TOKENS_URL, HttpMethod.POST, request, Map.class);
            Map<String, Object> resp = respEntity.getBody();
            Boolean ok = resp != null ? (Boolean) resp.get("status") : Boolean.FALSE;
            if (Boolean.TRUE.equals(ok)) {
                Map<String, Object> data = (Map<String, Object>) resp.get("data");
                if (data == null) throw new RuntimeException("refreshTokens returned no data");

                session.setAttribute("jwtToken", data.get("jwtToken"));
                session.setAttribute("refreshToken", data.get("refreshToken"));
                session.setAttribute("feedToken", data.get("feedToken"));

                log.info("refreshTokens: updated tokens for clientCode={}", session.getAttribute("clientCode"));
                return data;
            } else {
                throw new RuntimeException("refreshTokens failed: " + (resp != null ? resp.get("message") : "unknown"));
            }
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("refreshTokens HTTP error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        }
    }

    // -----------------------
    // Helpers (headers & retry)
    // -----------------------

    private HttpHeaders baseHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("X-UserType", "USER");
        headers.set("X-SourceID", "WEB");
        headers.set("X-ClientLocalIP", clientLocalIp);
        headers.set("X-ClientPublicIP", clientPublicIp);
        headers.set("X-MACAddress", macAddress);
        headers.set("X-PrivateKey", apiKey);
        return headers;
    }

    private HttpHeaders authHeaders(HttpSession session) {
        HttpHeaders headers = baseHeaders();
        String jwt = (String) session.getAttribute("jwtToken");
        if (jwt != null) headers.setBearerAuth(jwt);
        String clientCode = (String) session.getAttribute("clientCode");
        if (clientCode != null) headers.set("X-ClientCode", clientCode);
        return headers;
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<Map> exchangeWithRetry(String url, HttpMethod method, HttpEntity<?> entity, HttpSession session) {
        try {
            return restTemplate.exchange(url, method, entity, Map.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                log.info("exchangeWithRetry: 401 for {}, attempting refresh and retry", url);
                refreshTokens(session); // refresh then retry once

                HttpHeaders retryHeaders = authHeaders(session);
                HttpEntity<?> retryEntity = (entity != null && entity.getBody() != null)
                        ? new HttpEntity<>(entity.getBody(), retryHeaders)
                        : new HttpEntity<>(retryHeaders);

                return restTemplate.exchange(url, method, retryEntity, Map.class);
            }
            throw e;
        }
    }

    // -----------------------
    // PROFILE / RMS
    // -----------------------

    /**
     * Fetch profile and store it in session (key: "profile") and update clientCode if present.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getProfile(HttpSession session) {
        ensureJwt(session);
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(session));
        ResponseEntity<Map> resp = exchangeWithRetry(GET_PROFILE_URL, HttpMethod.GET, entity, session);
        Map<String, Object> body = resp.getBody();
        Boolean ok = body != null ? (Boolean) body.get("status") : Boolean.FALSE;
        if (Boolean.TRUE.equals(ok)) {
            Map<String, Object> data = (Map<String, Object>) body.get("data");
            session.setAttribute("profile", data);
            if (data != null && data.get("clientcode") != null) session.setAttribute("clientCode", data.get("clientcode").toString());
            return data != null ? data : new HashMap<>();
        } else {
            throw new RuntimeException("getProfile failed: " + (body != null ? body.get("message") : "unknown"));
        }
    }

    public Map<String, Object> getRms(HttpSession session) {
        ensureJwt(session);
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(session));
        ResponseEntity<Map> resp = exchangeWithRetry(GET_RMS_URL, HttpMethod.GET, entity, session);
        return resp.getBody();
    }

    // -----------------------
    // ORDERS / TRADES / LTP
    // -----------------------

    /**
     * Generic placeOrder - pass payload according to AngelOne docs.
     * Returns data map containing orderid/uniqueorderid on success.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> placeOrder(HttpSession session, Map<String, Object> payload) {
        ensureJwt(session);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, authHeaders(session));
        ResponseEntity<Map> resp = exchangeWithRetry(PLACE_ORDER_URL, HttpMethod.POST, entity, session);
        Map<String, Object> body = resp.getBody();
        Boolean ok = body != null ? (Boolean) body.get("status") : Boolean.FALSE;
        if (Boolean.TRUE.equals(ok)) {
            return (Map<String, Object>) body.get("data");
        } else {
            throw new RuntimeException("placeOrder failed: " + (body != null ? body.get("message") : "unknown"));
        }
    }

    /**
     * Short helper for a hard-coded sample order.
     */
    public Map<String, Object> placeSampleOrder(HttpSession session) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("variety", "NORMAL");
        payload.put("tradingsymbol", "SBIN-EQ");
        payload.put("symboltoken", "3045");
        payload.put("transactiontype", "BUY");
        payload.put("exchange", "NSE");
        payload.put("ordertype", "MARKET");
        payload.put("producttype", "INTRADAY");
        payload.put("duration", "DAY");
        payload.put("price", "0"); // market
        payload.put("quantity", "1");
        String clientCode = (String) session.getAttribute("clientCode");
        if (clientCode != null) payload.put("clientcode", clientCode);
        return placeOrder(session, payload);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> modifyOrder(HttpSession session, Map<String, Object> payload) {
        ensureJwt(session);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, authHeaders(session));
        ResponseEntity<Map> resp = exchangeWithRetry(MODIFY_ORDER_URL, HttpMethod.POST, entity, session);
        Map<String, Object> body = resp.getBody();
        if (body != null && Boolean.TRUE.equals(body.get("status"))) {
            return (Map<String, Object>) body.get("data");
        } else {
            throw new RuntimeException("modifyOrder failed: " + (body != null ? body.get("message") : "unknown"));
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> cancelOrder(HttpSession session, Map<String, Object> payload) {
        ensureJwt(session);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, authHeaders(session));
        ResponseEntity<Map> resp = exchangeWithRetry(CANCEL_ORDER_URL, HttpMethod.POST, entity, session);
        Map<String, Object> body = resp.getBody();
        if (body != null && Boolean.TRUE.equals(body.get("status"))) {
            return (Map<String, Object>) body.get("data");
        } else {
            throw new RuntimeException("cancelOrder failed: " + (body != null ? body.get("message") : "unknown"));
        }
    }

    public Map<String, Object> getOrderBook(HttpSession session) {
        ensureJwt(session);
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(session));
        ResponseEntity<Map> resp = exchangeWithRetry(GET_ORDER_BOOK_URL, HttpMethod.GET, entity, session);
        return resp.getBody();
    }

    public Map<String, Object> getTradeBook(HttpSession session) {
        ensureJwt(session);
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(session));
        ResponseEntity<Map> resp = exchangeWithRetry(GET_TRADE_BOOK_URL, HttpMethod.GET, entity, session);
        return resp.getBody();
    }

    public Map<String, Object> getOrderDetails(HttpSession session, String uniqueOrderId) {
        ensureJwt(session);
        String url = String.format(GET_ORDER_DETAILS_URL_TEMPLATE, uniqueOrderId);
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(session));
        ResponseEntity<Map> resp = exchangeWithRetry(url, HttpMethod.GET, entity, session);
        return resp.getBody();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getLtpData(HttpSession session, String exchange, String tradingsymbol, String symboltoken) {
        ensureJwt(session);
        Map<String, Object> payload = new HashMap<>();
        payload.put("exchange", exchange);
        payload.put("tradingsymbol", tradingsymbol);
        if (symboltoken != null) payload.put("symboltoken", symboltoken);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, authHeaders(session));
        ResponseEntity<Map> resp = exchangeWithRetry(GET_LTP_URL, HttpMethod.POST, entity, session);
        return resp.getBody();
    }
    // add near other endpoint constants

    /**
     * Fetch all holdings (portfolio summary + holdings list) and return the raw response map.
     * Stores nothing in session (read-only).
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getAllHoldings(HttpSession session) {
        ensureJwt(session); // throws if not logged in

        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(session));
        ResponseEntity<Map> resp = exchangeWithRetry(GET_ALL_HOLDING_URL, HttpMethod.GET, entity, session);

        Map<String, Object> body = resp.getBody();
        if (body == null) {
            throw new RuntimeException("Empty response from getAllHoldings");
        }
        // Usually the API wraps data under "data" — return the whole body so caller can inspect status/message/data
        return body;
    }
    /**
     * Search for a single scrip by ticker (equity only)
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> searchScrip(HttpSession session, String searchTicker) {
        ensureJwt(session);

        Map<String, String> payload = new HashMap<>();
        payload.put("exchange", "NSE");
        payload.put("searchscrip", searchTicker.toUpperCase());

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(payload, authHeaders(session));

        try {
            ResponseEntity<Map> resp = restTemplate.exchange(SEARCH_SCRIP_URL, HttpMethod.POST, entity, Map.class);
            Map<String, Object> body = resp.getBody();
            if (body != null && Boolean.TRUE.equals(body.get("status"))) {
                List<Map<String, Object>> data = (List<Map<String, Object>>) body.get("data");
                if (data == null) return Collections.emptyList();

                // Filter for equity only
                List<Map<String, Object>> eqList = new ArrayList<>();
                for (Map<String, Object> scrip : data) {
                    String symbol = (String) scrip.get("tradingsymbol");
                    if (symbol != null && symbol.toUpperCase().endsWith("-EQ")) {
                        eqList.add(scrip);
                    }
                }
                return eqList;
            } else {
                throw new RuntimeException("searchScrip failed: " + (body != null ? body.get("message") : "unknown"));
            }
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("searchScrip HTTP error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        }
    }
    /**
     * Get allowed NSE intraday scrips
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAllowedIntradayScrips(HttpSession session) {
        ensureJwt(session);

        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(session));
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(INTRADAY_NSE_URL, HttpMethod.GET, entity, Map.class);
            Map<String, Object> body = resp.getBody();
            if (body != null && Boolean.TRUE.equals(body.get("status"))) {
                return (List<Map<String, Object>>) body.get("data");
            } else {
                return Collections.emptyList();
            }
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("getAllowedIntradayScrips HTTP error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        }
    }
    // -----------------------
    // LOGOUT
    // -----------------------
    /**
     * Search scrip in NSE/BSE (returns first EQ instrument)
     */
    public Scrip searchScrip(HttpSession session, String exchange, String ticker) {
        ensureJwt(session);

        Map<String, String> payload = Map.of(
                "exchange", exchange,
                "searchscrip", ticker.toUpperCase()
        );

        HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, authHeaders(session));
        ResponseEntity<Map> response = restTemplate.exchange(SEARCH_SCRIP_URL, HttpMethod.POST, request, Map.class);
        Map<String, Object> body = response.getBody();

        if (body != null && Boolean.TRUE.equals(body.get("status"))) {
            List<Map<String, Object>> data = (List<Map<String, Object>>) body.get("data");
            if (data != null && !data.isEmpty()) {
                for (Map<String, Object> item : data) {
                    String sym = (String) item.get("tradingsymbol");
                    if (sym != null && sym.endsWith("-EQ")) {
                        Scrip scrip = new Scrip();
                        scrip.setExchange(exchange);
                        scrip.setTradingsymbol(sym);
                        scrip.setSymboltoken(String.valueOf(item.get("symboltoken")));
                        return scrip;
                    }
                }
            }
        }
        return null;
    }

    public void logout(HttpSession session) {
        String clientCode = (String) session.getAttribute("clientCode");
        if (clientCode == null) {
            session.invalidate();
            return;
        }
        Map<String, String> body = new HashMap<>();
        body.put("clientcode", clientCode);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, authHeaders(session));
        try {
            restTemplate.exchange(LOGOUT_URL, HttpMethod.POST, request, Map.class);
        } catch (Exception ignored) {
        } finally {
            session.invalidate();
        }
    }

    // -----------------------
    // Utilities
    // -----------------------

    private void ensureJwt(HttpSession session) {
        if (session.getAttribute("jwtToken") == null) {
            throw new RuntimeException("Not authenticated. Please login.");
        }
    }
}
