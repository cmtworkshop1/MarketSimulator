package com.stockprice.writer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.Map;

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

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;



public class StockPriceWriter {

    private static final Log LOG = LogFactory.getLog(StockPriceWriter.class);
    
    private static final DecimalFormat df = new DecimalFormat("###.##");

    private static void checkUsage(String[] args) {
        if (args.length != 7) {
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


    private static void sendStockPrice(String underLier, AmazonKinesis kinesisClient,
            String streamName) {
    	byte[] bytes = underLier.getBytes();
        // The bytes could be null if there is an issue with the JSON serialization by the Jackson JSON library.
        if (bytes == null) {
            LOG.warn("Could not get JSON bytes for stock trade");
            return;
        }
        
        LOG.info("Putting stock: " + underLier);
        PutRecordRequest putRecord = new PutRecordRequest();
        putRecord.setStreamName(streamName);
        putRecord.setPartitionKey(underLier);
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
        String underLier = args[2];
        String spot_price = args[3];
        String vol = args[4];
        String interestRate = args[5]; 
        String redisUrl = args[6];
        
        Region region = RegionUtils.getRegion(regionName);
        if (region == null) {
            System.err.println(regionName + " is not a valid AWS region.");
            System.exit(1);
        }
        
        if (streamName == null) {
            System.err.println(streamName + " is not a valid streamName.");
            System.exit(1);
        }
        
        if (underLier == null) {
            System.err.println(underLier + " is not a valid stock.");
            System.exit(1);
        }
        
        System.out.println("region:streamName:underLier-->"+region+":"+streamName+":"+underLier);
        
    
        AWSCredentials credentials = CredentialUtils.getCredentialsProvider().getCredentials();

        AmazonKinesis kinesisClient = new AmazonKinesisClient(credentials,
                ConfigurationUtils.getClientConfigWithUserAgent());
        kinesisClient.setRegion(region);


        validateStream(kinesisClient, streamName);
        
        Jedis jedisCluster = new Jedis(redisUrl, 6379);
        
        double lastPrice = 0;
        if(spot_price==null)
        	spot_price = "100.00";
        
        if(vol==null)
        	vol = "0.4";
        
        if(interestRate==null)
        	interestRate = "0.02";  
        
        jedisCluster.select(3);
        jedisCluster.hset(underLier, "spot_price",spot_price);
        jedisCluster.hset(underLier, "vol", vol);

     
		jedisCluster.select(4);
     	jedisCluster.hset(underLier,"interest_rate", interestRate);
     	sendStockPrice(underLier, kinesisClient, streamName);
        
		System.out.println("spot_price:vol:interestRate--> before loop"+spot_price+":"+vol+":"+interestRate);

        StockPriceGenerator stockPriceGenerator = new StockPriceGenerator(Double.valueOf(vol), Double.valueOf(interestRate),underLier,Double.valueOf(spot_price));
        
       
        while(true) {
        	Thread.sleep(1000);
            StockPrice stockprice = stockPriceGenerator.getRandomPrice();
            lastPrice = stockprice.getPrice();
            
            jedisCluster.select(3);
            jedisCluster.hset(underLier, "spot_price", df.format(lastPrice));
            spot_price = jedisCluster.hget(underLier, "spot_price");
            vol = jedisCluster.hget(underLier, "vol");
         
    		jedisCluster.select(4);
    		interestRate = jedisCluster.hget(underLier,"interest_rate");
         	
    		sendStockPrice(underLier, kinesisClient, streamName);
            System.out.println("spot_price:vol:interestRate--> after loop"+spot_price+":"+vol+":"+interestRate);
            
        }
        
    }

}
