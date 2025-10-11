package com.algotrade.bot.model;

import java.time.LocalDateTime;
import java.util.Map;

public class TradingViewAlert {
    private String id;
    private String ticker;
    private String exchange;
    private String interval;
    private String time;
    private String action;
    private int quantity = 1;
    private String status = "NEW"; // NEW / ACCEPTED / REJECTED / FAILED
    private LocalDateTime timestamp;
    private Map<String,Object> orderResult;
    private String errorMessage;
    private Map<String,Object> rawPayload;
    private String symboltoken; // optional if payload includes it

    public TradingViewAlert() {}

    // getters & setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }

    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }

    public String getInterval() { return interval; }
    public void setInterval(String interval) { this.interval = interval; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public Map<String, Object> getOrderResult() { return orderResult; }
    public void setOrderResult(Map<String, Object> orderResult) { this.orderResult = orderResult; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Map<String, Object> getRawPayload() { return rawPayload; }
    public void setRawPayload(Map<String, Object> rawPayload) { this.rawPayload = rawPayload; }

    public String getSymboltoken() { return symboltoken; }
    public void setSymboltoken(String symboltoken) { this.symboltoken = symboltoken; }
}
