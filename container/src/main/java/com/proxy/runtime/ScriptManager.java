package com.proxy.runtime;

import chat.errors.CoreException;
import chat.logs.LoggerEx;
import chat.utils.TimerEx;
import chat.utils.TimerTaskEx;
import chat.config.BaseConfiguration;
import com.docker.data.DeployServiceVersion;
import com.docker.data.DockerStatus;
import com.docker.data.ServiceVersion;
import com.docker.script.executor.RuntimeExecutor;
import com.docker.script.executor.RuntimeExecutorHandler;
import com.docker.storage.adapters.impl.DeployServiceVersionServiceImpl;
import com.docker.storage.adapters.impl.DockerStatusServiceImpl;
import com.docker.storage.adapters.impl.ServiceVersionServiceImpl;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import script.utils.ShutdownListener;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ScriptManager implements ShutdownListener {
    private static final String TAG = ScriptManager.class.getSimpleName();

    private RuntimeExecutor runtimeExecutor;
    @Autowired
    private BaseConfiguration baseConfiguration;
    boolean isShutdown = false;
    boolean isLoaded = false;
    @Autowired
    private ServiceVersionServiceImpl serviceVersionService;
    @Autowired
    private DockerStatusServiceImpl dockerStatusService;
    @Autowired
    private DeployServiceVersionServiceImpl deployServiceVersionService;
    public void init() {
        File dockerFile = new File(baseConfiguration.getLocalPath() + "/" + baseConfiguration.getDockerName());
        if (dockerFile.exists()) {
            File[] serviceFiles = dockerFile.listFiles();
            if(serviceFiles != null){
                for (File serviceFile : serviceFiles) {
                    try {
                        FileUtils.deleteDirectory(new File(baseConfiguration.getLocalPath() + "/" + baseConfiguration.getDockerName() + "/" + serviceFile.getName() + "/groovy"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        if (baseConfiguration.getHotDeployment()) {
            TimerEx.schedule(new TimerTaskEx(ScriptManager.class.getSimpleName()) {
                @Override
                public void execute() {
                    if (!isLoaded) {
                        synchronized (ScriptManager.this) {
                            if (!isShutdown && !isLoaded)
                                reload();
                        }
                    }
                }
            }, 5000L, 10000L);
        } else {
            reload();
        }
    }
    private void reload(){
        isLoaded = true;
        this.runtimeExecutor.executeAsync(baseConfiguration, new RuntimeExecutorHandler() {
            @Override
            public void handleSuccess() {
                isLoaded = false;
                try {
                    DeployServiceVersion deployServiceVersion = deployServiceVersionService.getServiceVersion(baseConfiguration.getServerType());
                    if(!baseConfiguration.getUseHulkAdmin()){
                        updateServiceVersion(deployServiceVersion);
                    }
                    DockerStatus dockerStatus = dockerStatusService.getDockerStatusByServer(baseConfiguration.getServer());
                    if (dockerStatus.getStatus() != DockerStatus.STATUS_OK) {
                        dockerStatus.setStatus(DockerStatus.STATUS_OK);
                        dockerStatusService.update(baseConfiguration.getServer(), dockerStatus);
                        LoggerEx.info(TAG, "================ This dockerStatus reload finish =======================");
                    }
                }catch (Throwable t){
                    handleFailed(t);
                }
            }

            @Override
            public void handleFailed(Throwable t, String service, Integer version) {
                if(baseConfiguration.getKillProcess()){
                    handleFailed(t);
                }else {
                    try {
                        dockerStatusService.deleteService(baseConfiguration.getServer(), service, version);
                    } catch (CoreException e) {
                        LoggerEx.error(TAG, "handleFailed failed, errMsg: " + ExceptionUtils.getFullStackTrace(e));
                    }
                }
            }

            @Override
            public void handleFailed(Throwable t) {
                LoggerEx.error(TAG, ExceptionUtils.getFullStackTrace(t));
                if (baseConfiguration.getKillProcess()) {
                    try {
                        DockerStatus dockerStatus = dockerStatusService.getDockerStatusByServer(baseConfiguration.getServer());
                        if (dockerStatus != null) {
                            dockerStatus.setStatus(DockerStatus.STATUS_FAILED);
                            dockerStatus.setFailedReason(t.getMessage());
                            dockerStatusService.update(baseConfiguration.getServer(), dockerStatus);
                        }
                    }catch (Throwable e){
                        LoggerEx.error(TAG, "handleFailed failed, errMsg: " + ExceptionUtils.getFullStackTrace(e));
                    }
                    System.exit(1);
                }
            }
        });

    }

    @Override
    public void shutdown() throws CoreException {
        isShutdown = true;
        dockerStatusService.deleteDockerStatus(baseConfiguration.getServer());
        baseConfiguration.close();
    }

    private void updateServiceVersion(DeployServiceVersion deployServiceVersion) {
        try {
            List<ServiceVersion> serviceVersionList = serviceVersionService.getServiceVersionsByType(deployServiceVersion.getServerType(), deployServiceVersion.getType());
            if (serviceVersionList != null && !serviceVersionList.isEmpty()) {
                for (ServiceVersion serviceVersion : serviceVersionList) {
                    serviceVersion.setServiceVersions(deployServiceVersion.getServiceVersions());
                    serviceVersion.setDeployId(deployServiceVersion.getDeployId());
                    serviceVersionService.addServiceVersion(serviceVersion);
                }
            } else {
                ServiceVersion serviceVersion = new ServiceVersion();
                List<String> list = new ArrayList<>();
                list.add(deployServiceVersion.getServerType());
                serviceVersion.setServerType(list);
                serviceVersion.set_id(deployServiceVersion.get_id());
                serviceVersion.setType(deployServiceVersion.getType());
                serviceVersion.setServiceVersions(deployServiceVersion.getServiceVersions());
                serviceVersion.setDeployId(deployServiceVersion.getDeployId());
                serviceVersionService.addServiceVersion(serviceVersion);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public void setRuntimeExecutor(RuntimeExecutor runtimeExecutor) {
        this.runtimeExecutor = runtimeExecutor;
    }

    public void setBaseConfiguration(BaseConfiguration baseConfiguration) {
        this.baseConfiguration = baseConfiguration;
    }
}
