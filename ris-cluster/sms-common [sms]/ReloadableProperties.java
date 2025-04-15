package com.pearson.sms.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.Properties;

public abstract class ReloadableProperties {
    private static final Logger logger = LoggerFactory.getLogger(ReloadableProperties.class.getName());
    private static final String LOAD_ERR_MSG_TEMPLATE = "Failed to load properties from %s";
    private static final String FILE_ERR_MSG_TEMPLATE = "Can't find properties file from class path %s";
    private static final long TIME_INTERVAL = 300000L;
    private volatile Properties properties = null;
    private String fileName = null;
    private volatile long lastModTimeOfFile = 0L;
    private volatile long lastTimeChecked = 0L;
    private File file = null;
    private URL url = null;

    protected void init(String fileName) {
        this.fileName = fileName;
        url = this.getClass().getClassLoader().getResource(this.fileName);
        initOrReloadIfNeeded();
    }

    private void initOrReloadIfNeeded() {
        logger.info("Inside Properties Init/Load | Start | "+fileName);
        long currentTs = System.currentTimeMillis();
        if ((lastTimeChecked + TIME_INTERVAL) > currentTs)
            return;

        if (null == url) {
            logger.error(String.format(FILE_ERR_MSG_TEMPLATE, fileName));
            throw new RuntimeException(String.format(FILE_ERR_MSG_TEMPLATE, fileName));
        }
        try {
            if(null == file){
                file = new File(url.getFile());
            }

            long currModTime = file.lastModified();

            if (currModTime > lastModTimeOfFile) {
                logger.info("Loading Properties | "+fileName);
                if(null == properties){
                    properties = new Properties();
                }
                properties.load(url.openStream());
                lastModTimeOfFile = currModTime;
                logger.info("Loaded Properties | "+fileName);
            }
            lastTimeChecked = currentTs;
        } catch (Exception e) {
            logger.error(String.format(LOAD_ERR_MSG_TEMPLATE, url.getPath()), e);
            throw new RuntimeException(e.getCause());
        }
        logger.info("Inside Properties Init/Load | End | "+fileName);
    }

    public String getString(String param) {
        initOrReloadIfNeeded();
        return properties.getProperty(param);
    }
}
