package com.stockprice.model;

import java.io.IOException;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;


public class StockPrice {

    private final static ObjectMapper JSON = new ObjectMapper();
    static {
        JSON.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private String tickerSymbol;
    private double price;
    private long quantity;
    private long id;

    public StockPrice() {
    }

    public StockPrice(String tickerSymbol,double price, long quantity, long id) {
        this.tickerSymbol = tickerSymbol;
        this.price = price;
        this.quantity = quantity;
        this.id = id;
    }

    public String getTickerSymbol() {
        return tickerSymbol;
    }

    public double getPrice() {
        return price;
    }

    public long getQuantity() {
        return quantity;
    }

    public long getId() {
        return id;
    }

    public byte[] toJsonAsBytes() {
        try {
            return JSON.writeValueAsBytes(this);
        } catch (IOException e) {
            return null;
        }
    }

    public static StockPrice fromJsonAsBytes(byte[] bytes) {
        try {
            return JSON.readValue(bytes, StockPrice.class);
        } catch (IOException e) {
            return null;
        }
    }
}
