package com.container.runtime.executor.serviceversion;

import chat.errors.ChatErrorCodes;
import chat.errors.CoreException;
import chat.config.BaseConfiguration;
import chat.config.Configuration;
import chat.logs.LoggerEx;
import com.docker.data.DeployServiceVersion;
import com.docker.script.executor.serviceversion.ServiceVersionsHandler;
import com.docker.storage.adapters.DeployServiceVersionService;
import com.docker.storage.adapters.DockerStatusService;
import com.docker.storage.adapters.impl.DeployServiceVersionServiceImpl;
import com.docker.storage.adapters.impl.DockerStatusServiceImpl;
import com.docker.utils.BeanFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by lick on 2020/12/17.
 * Description：
 */
public class DefaultServiceVersionsHandler implements ServiceVersionsHandler {
    private final String TAG = DeployServiceVersionServiceImpl.class.getName();

    @Override
    public Map<String, Configuration> generateConfigurations(BaseConfiguration baseConfiguration) throws CoreException {
        DeployServiceVersionService deployServiceVersionService = (DeployServiceVersionService) BeanFactory.getBean(DeployServiceVersionServiceImpl.class.getName());
        DockerStatusService dockerStatusService = (DockerStatusService)BeanFactory.getBean(DockerStatusServiceImpl.class.getName());
        DeployServiceVersion deployServiceVersion = deployServiceVersionService.getServiceVersion(baseConfiguration.getServerType());
        if(deployServiceVersion == null){
            throw new CoreException(ChatErrorCodes.ERROR_DEPLOYSERVICEVERSION_NULL, "Cant find deployserviceversion, serverType: " + baseConfiguration.getServerType());
        }
        if (deployServiceVersion.getDeployId() != null) {
            dockerStatusService.updateDeployId(baseConfiguration.getServer(), deployServiceVersion.getDeployId());
        }
        Map<String, String> serviceVersions = deployServiceVersion.getServiceVersions();
        if (serviceVersions == null || serviceVersions.isEmpty()) {
            throw new CoreException(ChatErrorCodes.ERROR_SERVICEVERSION_EMPTY, "Serviceversion is empty, serverType: " + baseConfiguration.getServerType());
        }
        Map<String, Configuration> configurationMap = new HashMap<>();
        for (String service : serviceVersions.keySet()) {
            Configuration configuration = new Configuration();
            configuration.setService(service);
            String version = serviceVersions.get(service);
            /**
             *             "/scripts/testjavaoceanus_v1#Java/java.zip"
             *             "/scripts/testjavaoceanus_v1#Jar/java.jar"
             *             "/scripts/testjavaoceanus_v1/groovy.zip"
             */
            configuration.setLanguageType(Configuration.LANGEUAGE_GROOVY);
            String[] strings = version.split("#");
            if(strings.length == 2){
                if(strings[1].toLowerCase().equals(Configuration.LANGEUAGE_JAVA.toLowerCase())){
                    configuration.setLanguageType(Configuration.LANGEUAGE_JAVA);
                }else if(strings[1].toLowerCase().equals(Configuration.LANGEUAGE_JAVA_JAR.toLowerCase())){
                    configuration.setLanguageType(Configuration.LANGEUAGE_JAVA_JAR);
                }
            }
            configuration.setVersion(Integer.valueOf(strings[0]));
            configuration.setBaseConfiguration(baseConfiguration);
            Configuration oldConfiguration = configurationMap.putIfAbsent(service, configuration);
            if(oldConfiguration != null){
                LoggerEx.warn(TAG, "Service " + service + " is duplicate, will drop, configuration: " + configuration);
            }
        }
        return configurationMap;
    }
}
