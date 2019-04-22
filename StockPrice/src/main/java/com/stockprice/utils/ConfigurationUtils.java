package com.stockprice.utils;

import com.amazonaws.ClientConfiguration;


public class ConfigurationUtils {

    private static final String APPLICATION_NAME = "amazon-kinesis";
    private static final String VERSION = "1.0.0";

    public static ClientConfiguration getClientConfigWithUserAgent() {
        final ClientConfiguration config = new ClientConfiguration();
        final StringBuilder userAgent = new StringBuilder(ClientConfiguration.DEFAULT_USER_AGENT);


        userAgent.append(" ");
       
        userAgent.append(APPLICATION_NAME);
        userAgent.append("/");
        userAgent.append(VERSION);

        config.setUserAgent(userAgent.toString());

        return config;
    }

}
