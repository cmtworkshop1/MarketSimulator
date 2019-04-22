package com.stockprice.writer;


import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import com.stockprice.model.StockPrice;

public class StockPriceGenerator {

	private final Random random = new Random();
    private AtomicLong id = new AtomicLong(1);
    double r = 0.02;
    double vol = 0.4;
    double dt = 1.0/250.0;
    double a = (r - (0.5*Math.pow(vol, 2)))*dt;
    double b = vol*Math.sqrt(dt);    
    
    public StockPrice getRandomPrice(double lastPrice) {
    	
    	lastPrice = lastPrice*Math.exp(a
                + (b*random.nextGaussian()));
    
    	
        return new StockPrice("IBM",lastPrice, 100, id.getAndIncrement());
    }

}
