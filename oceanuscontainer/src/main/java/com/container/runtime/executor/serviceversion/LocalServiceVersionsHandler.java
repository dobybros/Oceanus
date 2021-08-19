package com.container.runtime.executor.serviceversion;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import com.container.errors.ContainerErrorCodes;
import com.docker.script.executor.serviceversion.ServiceVersionsHandler;

import chat.config.BaseConfiguration;
import chat.config.Configuration;
import chat.logs.LoggerEx;

/**
 * Created by lick on 2020/12/17.
 * Description：
 */
public class LocalServiceVersionsHandler implements ServiceVersionsHandler {
    private final String TAG = LocalServiceVersionsHandler.class.getName();

    @Override
    public Map<String, Configuration> generateConfigurations(BaseConfiguration baseConfiguration) throws CoreException {
        String remotePath = baseConfiguration.getRemotePath();
        File directory = new File(remotePath);
        if (!directory.isDirectory()) {
            try {
                FileUtils.forceMkdir(directory);
            } catch (IOException e) {
                e.printStackTrace();
                throw new CoreException(ContainerErrorCodes.ERROR_SERVICE_VERSIONS_CREATE_REMOTE_PATH_FAILED, "Force create directory " + directory + " failed, " + e.getMessage());
            } catch (Throwable t) {
                throw new CoreException(ContainerErrorCodes.ERROR_SERVICE_VERSIONS_CREATE_REMOTE_PATH_FAILED, "Force create directory " + directory + " failed (unknown), " + t.getMessage());
            }
        }
        LinkedHashMap<String, Configuration> configurationMap = new LinkedHashMap<>();
        Collection<File> groovyZipFiles = FileUtils.listFiles(directory, new String[]{"zip"}, true);
        List<Configuration> configurations = new ArrayList<>();
        Configuration discoveryConfiguration = null;
        for (File groovyZipFile : groovyZipFiles) {
            String path = groovyZipFile.getAbsolutePath().substring(directory.getAbsolutePath().length());
            if (path.startsWith(File.separator)) {
                path = path.substring(File.separator.length());
            }
            path = path.replace("\\", "/");
            String[] strs = path.split("/");
            String service = null;
            Integer version = null;
            if (strs.length == 2) {
                if (strs[0].contains("_")) {
                    int pos = strs[0].lastIndexOf("_v");
                    service = strs[0].substring(0, pos);
                    String versionStr = strs[0].substring(pos + 2);
                    try {
                        version = Integer.parseInt(versionStr);
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                        throw new CoreException(ContainerErrorCodes.ERROR_SERVICE_VERSIONS_GROOVY_ZIP_VERSION_ILLEGAL, "groovy.zip path version is illegal, ignored. " + groovyZipFile.getAbsolutePath());
                    }
                }
            } else {
                throw new CoreException(ContainerErrorCodes.ERROR_SERVICE_VERSIONS_GROOVY_ZIP_PATH_ILLEGAL, "groovy.zip path is illegal, ignored. " + groovyZipFile.getAbsolutePath());
            }
            Configuration configuration = new Configuration();
            configuration.setService(service);
            configuration.setVersion(version);
            configuration.setBaseConfiguration(baseConfiguration);
            if (service.equals("discovery")) {
                discoveryConfiguration = configuration;
            } else {
                configurations.add(configuration);
            }
        }
        if (discoveryConfiguration != null) {
            configurations.add(0, discoveryConfiguration);
        }

        for (Configuration configuration : configurations) {
            Configuration old = configurationMap.get(configuration.getService());
            if (old != null) {
                if (old.getVersion() > configuration.getVersion()) {
                    LoggerEx.warn(TAG, "Found smaller version of service " + configuration.getService() + " version " + configuration.getVersion() + " will NOT replace old version " + old.getVersion() + " because old is newer version");
                } else {
                    configurationMap.put(configuration.getService(), configuration);
                    LoggerEx.warn(TAG, "Found bigger version of service " + configuration.getService() + " version " + configuration.getVersion() + " replace old version " + old.getVersion());
                }
            } else {
                configurationMap.put(configuration.getService(), configuration);
//                LoggerEx.info(TAG, "Found service " + service + " version " + version);
            }
        }
        return configurationMap;
    }

}
