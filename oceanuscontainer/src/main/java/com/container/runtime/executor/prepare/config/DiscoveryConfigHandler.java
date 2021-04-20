package com.container.runtime.executor.prepare.config;

import chat.config.Configuration;
import chat.logs.LoggerEx;
import com.docker.data.DataObject;
import com.docker.oceansbean.BeanFactory;
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
                for (Object key : properties.keySet()) {
                    Object value = properties.get(key);
                    if (value instanceof String) {
                        if (StringUtils.isBlank((String) value)) {
                            properties.remove(key);
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
