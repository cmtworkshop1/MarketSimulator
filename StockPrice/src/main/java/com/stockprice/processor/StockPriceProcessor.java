package com.stockprice.processor;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;
import com.stockprice.utils.ConfigurationUtils;
import com.stockprice.utils.CredentialUtils;


public class StockPriceProcessor {

    private static final Log LOG = LogFactory.getLog(StockPriceProcessor.class);

    private static final Logger ROOT_LOGGER = Logger.getLogger("");
    private static final Logger PROCESSOR_LOGGER =
            Logger.getLogger("com.stockprice.processor");

    private static void checkUsage(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: " + StockPriceProcessor.class.getSimpleName()
                    + " <application name> <stream name> <region>");
            System.exit(1);
        }
    }


    private static void setLogLevels() {
        ROOT_LOGGER.setLevel(Level.WARNING);
        PROCESSOR_LOGGER.setLevel(Level.INFO);
    }

    public static void main(String[] args) throws Exception {
        checkUsage(args);

        String applicationName = args[0];
        String streamName = args[1];
        Region region = RegionUtils.getRegion(args[2]);
        if (region == null) {
            System.err.println(args[2] + " is not a valid AWS region.");
            System.exit(1);
        }

        setLogLevels();

        AWSCredentialsProvider credentialsProvider = CredentialUtils.getCredentialsProvider();

        String workerId = String.valueOf(UUID.randomUUID());
        KinesisClientLibConfiguration kclConfig =
                new KinesisClientLibConfiguration(applicationName, streamName, credentialsProvider, workerId)
            .withRegionName(region.getName())
            .withCommonClientConfig(ConfigurationUtils.getClientConfigWithUserAgent());

        IRecordProcessorFactory recordProcessorFactory = new StockPriceRecordProcessorFactory();

        // Create the KCL worker with the stock trade record processor factory
        Worker worker = new Worker(recordProcessorFactory, kclConfig);

        int exitCode = 0;
        try {
            worker.run();
        } catch (Throwable t) {
            LOG.error("Caught throwable while processing data.", t);
            exitCode = 1;
        }
        System.exit(exitCode);

    }

}
