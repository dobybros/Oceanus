package com.container.runtime.executor.prepare.config;

import chat.config.BaseConfiguration;
import chat.config.Configuration;
import chat.logs.LoggerEx;
import com.docker.script.executor.prepare.config.ConfigHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by lick on 2020/12/18.
 * Description：
 */
public class DiscoveryConfigHandler implements ConfigHandler {
    private final String TAG = DiscoveryConfigHandler.class.getSimpleName();
    @Override
    public void prepare(Configuration configuration) throws IOException {
        //TODO Aplomb
        String propertiesPath = configuration.getLocalPath() + File.separator + configuration.getBaseConfiguration().getDefaultConfigFileName();
        Properties properties = new Properties();
        File propertiesFile = new File(propertiesPath);
        if (propertiesFile.exists() && propertiesFile.isFile()) {
            try (InputStream is = FileUtils.openInputStream(propertiesFile)){
                properties.load(is);
            }
        }
        try {
            if (!properties.isEmpty()) {
                BaseConfiguration baseConfiguration = configuration.getBaseConfiguration();
                Properties envConfigProperties = null;
                if(baseConfiguration != null) {
                    envConfigProperties = baseConfiguration.getEnvConfigProperties();
                }
                for (Object key : properties.keySet()) {
                    boolean ignoreKey = false;
                    Object value = properties.get(key);
                    if (value instanceof String) {
                        if (StringUtils.isBlank((String) value)) {
                            properties.remove(key);
                            ignoreKey = true;
                        }
                    }
                    if(envConfigProperties != null && !ignoreKey) {
                        Object newValue = envConfigProperties.get(key);
                        if(newValue == null) {
                            newValue = envConfigProperties.getProperty(configuration.getService() + "/" + key);
                        }
                        if(newValue != null) {
                            Object old = properties.put(key, newValue);
                            LoggerEx.info(TAG, "Service " + configuration.getService() + " override config key " + key + " from old " + old + " to new " + newValue);
                        }
                    }
                }
            }
            configuration.setConfig(properties);
            LoggerEx.info(TAG, "Read service: " + configuration.getServiceVersion() + ", merge config: " + properties);
        } catch (Throwable t) {
            LoggerEx.error(TAG, "Read service " + configuration.getServiceVersion() + " config failed, " + ExceptionUtils.getFullStackTrace(t));
        }
    }
}
