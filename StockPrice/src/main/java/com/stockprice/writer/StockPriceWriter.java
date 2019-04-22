package com.stockprice.writer;

import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClient;
import com.amazonaws.services.kinesis.model.DescribeStreamResult;
import com.amazonaws.services.kinesis.model.PutRecordRequest;
import com.amazonaws.services.kinesis.model.ResourceNotFoundException;
import com.stockprice.model.StockPrice;
import com.stockprice.utils.ConfigurationUtils;
import com.stockprice.utils.CredentialUtils;


public class StockPriceWriter {

    private static final Log LOG = LogFactory.getLog(StockPriceWriter.class);

    private static void checkUsage(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: " + StockPriceWriter.class.getSimpleName()
                    + " <stream name> <region>");
            System.exit(1);
        }
    }


    private static void validateStream(AmazonKinesis kinesisClient, String streamName) {
        try {
            DescribeStreamResult result = kinesisClient.describeStream(streamName);
            if(!"ACTIVE".equals(result.getStreamDescription().getStreamStatus())) {
                System.err.println("Stream " + streamName + " is not active. Please wait a few moments and try again.");
                System.exit(1);
            }
        } catch (ResourceNotFoundException e) {
            System.err.println("Stream " + streamName + " does not exist. Please create it in the console.");
            System.err.println(e);
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Error found while describing the stream " + streamName);
            System.err.println(e);
            System.exit(1);
        }
    }


    private static void sendStockPrice(StockPrice stockprice, AmazonKinesis kinesisClient,
            String streamName) {
    	byte[] bytes = stockprice.toJsonAsBytes();
        // The bytes could be null if there is an issue with the JSON serialization by the Jackson JSON library.
        if (bytes == null) {
            LOG.warn("Could not get JSON bytes for stock trade");
            return;
        }
        
        LOG.info("Putting stock: " + stockprice.toString());
        PutRecordRequest putRecord = new PutRecordRequest();
        putRecord.setStreamName(streamName);
        putRecord.setPartitionKey(stockprice.getTickerSymbol());
        putRecord.setData(ByteBuffer.wrap(bytes));

        try {
            kinesisClient.putRecord(putRecord);
        } catch (AmazonClientException ex) {
            LOG.warn("Error sending record to Amazon Kinesis.", ex);
        }
    }

    public static void main(String[] args) throws Exception {
        checkUsage(args);

        String streamName = args[0];
        String regionName = args[1];
        Region region = RegionUtils.getRegion(regionName);
        if (region == null) {
            System.err.println(regionName + " is not a valid AWS region.");
            System.exit(1);
        }

        AWSCredentials credentials = CredentialUtils.getCredentialsProvider().getCredentials();

        AmazonKinesis kinesisClient = new AmazonKinesisClient(credentials,
                ConfigurationUtils.getClientConfigWithUserAgent());
        kinesisClient.setRegion(region);


        validateStream(kinesisClient, streamName);

        // Repeatedly send stock trades with a 100 milliseconds wait in between
        StockPriceGenerator stockPriceGenerator = new StockPriceGenerator();
        double lastPrice = 100;
        while(true) {
            StockPrice stockprice = stockPriceGenerator.getRandomPrice(lastPrice);
            sendStockPrice(stockprice, kinesisClient, streamName);
            Thread.sleep(1000);
            lastPrice = stockprice.getPrice();
            
        }
    }

}
