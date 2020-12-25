package com.docker.script.i18n;


import lombok.Setter;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by zhanjing on 2017/7/17.
 */
public class MessageProperties {
    public String getMessage(String key) {
        return getMessage(key, null, null);
    }
    public String getMessage(String key, String[] parameters) {
        return getMessage(key, parameters, null);
    }
    private Properties properties;
    public void setAbsolutePath(String absolutePath) {
        if(absolutePath != null){
            try (InputStream inputStream = FileUtils.openInputStream(new File(absolutePath))){
                properties.load(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String getMessage(String key, String[] parameters, String defaultValue) {
        if(properties != null){
            String value = properties.getProperty(key, defaultValue);
            if (parameters != null && parameters.length > 0) {
                for (int i = 0; i < parameters.length; i++) {
                    if (value.contains("#{" + i + "}"))
                        value = value.replace("#{" + i + "}", parameters[i]);
                }
            }
            return value;
        }
        return null;
    }

}
