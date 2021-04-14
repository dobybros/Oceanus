package com.container.runtime.executor.serviceversion;

import chat.config.BaseConfiguration;
import chat.config.Configuration;
import chat.errors.ChatErrorCodes;
import chat.errors.CoreException;
import chat.logs.LoggerEx;
import com.container.errors.ContainerErrorCodes;
import com.docker.data.DeployServiceVersion;
import com.docker.oceansbean.BeanFactory;
import com.docker.script.executor.serviceversion.ServiceVersionsHandler;
import com.docker.storage.adapters.DeployServiceVersionService;
import com.docker.storage.adapters.DockerStatusService;
import com.docker.storage.adapters.impl.DeployServiceVersionServiceImpl;
import com.docker.storage.adapters.impl.DockerStatusServiceImpl;
import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by lick on 2020/12/17.
 * Description：
 */
public class LocalServiceVersionsHandler implements ServiceVersionsHandler {
    private final String TAG = DeployServiceVersionServiceImpl.class.getName();
    @Override
    public Map<String, Configuration> generateConfigurations(BaseConfiguration baseConfiguration) throws CoreException {
        String remotePath = baseConfiguration.getRemotePath();
        File directory = new File(remotePath);
        if(!directory.isDirectory()) {
            try {
                FileUtils.forceMkdir(directory);
            } catch (IOException e) {
                e.printStackTrace();
                throw new CoreException(ContainerErrorCodes.ERROR_SERVICE_VERSIONS_CREATE_REMOTE_PATH_FAILED, "Force create directory " + directory + " failed, " + e.getMessage());
            } catch (Throwable t) {
                throw new CoreException(ContainerErrorCodes.ERROR_SERVICE_VERSIONS_CREATE_REMOTE_PATH_FAILED, "Force create directory " + directory + " failed (unknown), " + t.getMessage());
            }
        }
        Map<String, Configuration> configurationMap = new HashMap<>();
        Collection<File> groovyZipFiles = FileUtils.listFiles(directory, new String[]{"zip"}, true);
        for(File groovyZipFile : groovyZipFiles) {
            String path = groovyZipFile.getAbsolutePath().substring(directory.getAbsolutePath().length());
            if(path.startsWith(File.separator)) {
                path = path.substring(File.separator.length());
            }
            String[] strs = path.split(File.separator);
            String service = null;
            Integer version = null;
            if(strs.length == 2) {
                if(strs[0].contains("_")) {
                    int pos = strs[0].lastIndexOf("_v");
                    service = strs[0].substring(0, pos);
                    String versionStr = strs[0].substring(pos + 2);
                    try {
                        version = Integer.parseInt(versionStr);
                    } catch(Throwable throwable) {
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
            Configuration old = configurationMap.get(service);
            if(old != null) {
                if(old.getVersion() > configuration.getVersion()) {
                    LoggerEx.warn(TAG, "Found smaller version of service " + service + " version " + version + " will NOT replace old version " + old.getVersion() + " because old is newer version");
                } else {
                    configurationMap.put(service, configuration);
                    LoggerEx.warn(TAG, "Found bigger version of service " + service + " version " + version + " replace old version " + old.getVersion());
                }
            } else {
                configurationMap.put(service, configuration);
//                LoggerEx.info(TAG, "Found service " + service + " version " + version);
            }
        }
        return configurationMap;
    }

}
