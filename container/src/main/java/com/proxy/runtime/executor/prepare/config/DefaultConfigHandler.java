package com.proxy.runtime.executor.prepare.config;

import chat.errors.CoreException;
import chat.logs.LoggerEx;
import chat.config.Configuration;
import com.docker.data.DataObject;
import com.docker.data.Service;
import com.docker.script.executor.prepare.config.ConfigHandler;
import com.docker.storage.adapters.DockerStatusService;
import com.docker.storage.adapters.ServersService;
import com.docker.storage.adapters.impl.DockerStatusServiceImpl;
import com.docker.storage.adapters.impl.ServersServiceImpl;
import com.docker.utils.BeanFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.bson.Document;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by lick on 2020/12/18.
 * Description：
 */
public class DefaultConfigHandler implements ConfigHandler {
    private final String TAG = ConfigHandler.class.getSimpleName();
    @Override
    public void prepare(Configuration configuration) throws IOException {
        ServersService serversService = (ServersService) BeanFactory.getBean(ServersServiceImpl.class.getName());
        String propertiesPath = configuration.getLocalPath() + File.separator + configuration.getBaseConfiguration().getDefaultConfigFileName();
        Properties properties = new Properties();
        File propertiesFile = new File(propertiesPath);
        if (propertiesFile.exists() && propertiesFile.isFile()) {
            try (InputStream is = FileUtils.openInputStream(propertiesFile)){
                properties.load(is);
            }
        }
        try {
            Document configDoc = serversService.getServerConfig(configuration.getService());
            if (configDoc != null) {
                String configDependencies = configDoc.getString(configuration.getBaseConfiguration().getDefaultConfigDependencyFileName());
                Map<String, Object> configDependencyMap = null;
                if (configDependencies != null) {
                    String[] theConfigDependencies = configDependencies.split(",");
                    if (theConfigDependencies.length > 0) {
                        configDependencyMap = new ConcurrentHashMap<>();
                        for (int i = theConfigDependencies.length - 1; i >= 0; i--) {
                            Document configDependencyDoc = serversService.getServerConfig(theConfigDependencies[i]);
                            if (configDependencyDoc != null) {
                                configDependencyMap.putAll(configDependencyDoc);
                            }
                        }
                    }
                }
                if (configDependencyMap == null) {
                    configDependencyMap = configDoc;
                } else {
                    configDependencyMap.putAll(configDoc);
                }
                configDependencyMap.remove(DataObject.FIELD_ID);
                Set<String> keys = configDependencyMap.keySet();
                for (String key : keys) {
                    properties.put(key.replaceAll("_", "."), configDependencyMap.get(key));
                }
            }
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
            //add service to dockerStatus
            updateService(configuration);
            LoggerEx.info(TAG, "Read service: " + configuration.getServiceVersion() + ", merge config: " + properties);
        } catch (Throwable t) {
            LoggerEx.error(TAG, "Read service " + configuration.getServiceVersion() + " config failed, " + ExceptionUtils.getFullStackTrace(t));
        }
    }
    private void updateService(Configuration configuration) throws CoreException {
        DockerStatusService dockerStatusService = (DockerStatusService) BeanFactory.getBean(DockerStatusServiceImpl.class.getName());
        Service service = new Service();
        service.setService(configuration.getService());
        service.setVersion(configuration.getVersion());
        service.setUploadTime(configuration.getDeployVersion());
        if(configuration.getConfig().get(Service.FIELD_MAXUSERNUMBER) != null){
            service.setMaxUserNumber(Long.parseLong((String) configuration.getConfig().get(Service.FIELD_MAXUSERNUMBER)));
        }
        dockerStatusService.deleteService(configuration.getBaseConfiguration().getServer(), service.getService(), service.getVersion());
        dockerStatusService.addService(configuration.getBaseConfiguration().getServer(), service);
    }
}
