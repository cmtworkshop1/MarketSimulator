package com.stockprice.writer;


import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import com.stockprice.model.StockPrice;

public class StockPriceGenerator {

	private final Random random = new Random();
    private AtomicLong id = new AtomicLong(1);
    double r = 0;
    double vol = 0;
    double dt = 0;
    double a = 0;
    double b = 0;
    String underLier;
    double lastPrice;
    
    public StockPriceGenerator(double volatility, double rate, String under_Lier, double last_price)
    {
    	r = rate;
    	vol = volatility;
    	dt = 1.0/(250.0*9*60*60);
        a = (r - (0.5*Math.pow(vol, 2)))*dt;
        b = vol*Math.sqrt(dt);
        
        underLier = under_Lier;
        lastPrice = last_price;
    }
    
    public StockPrice getRandomPrice() {
    	
    	lastPrice = lastPrice*Math.exp(a
                + (b*random.nextGaussian()));
    
    	
        return new StockPrice(underLier,lastPrice, 100, id.getAndIncrement());
    }

}
