package com.stockprice.processor;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;


public class StockPriceRecordProcessorFactory implements IRecordProcessorFactory {


    public StockPriceRecordProcessorFactory() {
        super();
    }


    @Override
    public IRecordProcessor createProcessor() {
        return new StockPriceRecordProcessor();
    }

}
