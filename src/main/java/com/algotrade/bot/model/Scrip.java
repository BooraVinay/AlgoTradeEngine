package com.algotrade.bot.model;

public class Scrip {
    private String exchange;
    private String tradingsymbol;
    private String symboltoken;

    public Scrip() {}
    public Scrip(String exchange, String tradingsymbol, String symboltoken) {
        this.exchange = exchange;
        this.tradingsymbol = tradingsymbol;
        this.symboltoken = symboltoken;
    }

    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }

    public String getTradingsymbol() { return tradingsymbol; }
    public void setTradingsymbol(String tradingsymbol) { this.tradingsymbol = tradingsymbol; }

    public String getSymboltoken() { return symboltoken; }
    public void setSymboltoken(String symboltoken) { this.symboltoken = symboltoken; }
}
