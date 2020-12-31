package com.docker.server;

import chat.errors.CoreException;
import chat.logs.LoggerEx;
import chat.main.ServerStart;
import chat.utils.ChatUtils;
import chat.utils.IPHolder;
import chat.config.BaseConfiguration;
import com.docker.data.DockerStatus;
import com.docker.storage.adapters.DockerStatusService;
import com.docker.storage.adapters.SDockersService;
import com.docker.storage.cache.CacheStorageFactory;
import com.docker.storage.cache.CacheStorageMethod;
import com.docker.tasks.Task;
import connectors.mongodb.MongoClientFactory;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OnlineServer {
    private static final String TAG = OnlineServer.class.getSimpleName();
    private List<Task> tasks;
    private IPHolder ipHolder;
    private SDockersService sdockersService;

    private DockerStatusService dockerStatusService;

    private static OnlineServer instance;

    private DockerStatus dockerStatus;

    private OnlineServerStartHandler startHandler;
    @Autowired
    protected BaseConfiguration baseConfiguration;

    public static interface OnlineServerStartHandler {
        public void serverWillStart(OnlineServer onlineServer) throws CoreException;

        public void serverWillShutdown(OnlineServer onlineServer);
    }

    protected OnlineServer() {
        instance = this;
    }

    public static OnlineServer getInstance() {
        return instance;
    }

    public void prepare() {
    }

    public String getIp() {
        if (ipHolder != null)
            return ipHolder.getIp();
        return null;
    }

    public void setIpHolder(IPHolder ipHolder) {
        this.ipHolder = ipHolder;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }

    public void setSdockersService(SDockersService sdockersService) {
        this.sdockersService = sdockersService;
    }

    public void setDockerStatusService(DockerStatusService dockerStatusService) {
        this.dockerStatusService = dockerStatusService;
    }

    protected DockerStatus generateDockerStatus(Integer port) {
        DockerStatus dockerStatus = new DockerStatus();
        dockerStatus.setServer(baseConfiguration.getServer());
        dockerStatus.setServerType(baseConfiguration.getServerType());
        dockerStatus.setDockerName(baseConfiguration.getDockerName());
        dockerStatus.setIp(ipHolder.getIp());
        dockerStatus.setType(baseConfiguration.getType());
        dockerStatus.setMaxUserNumber(baseConfiguration.getMaxUserNumber());
        dockerStatus.setRpcPort(baseConfiguration.getRpcPort());
        dockerStatus.setSslRpcPort(baseConfiguration.getSslRpcPort());
        dockerStatus.setHttpPort(port);
        dockerStatus.setLanId(baseConfiguration.getLanId());
        if (baseConfiguration.getScaleInstanceId() != null) {
            dockerStatus.setHealth(DockerStatus.HEALTH_MAX);
        } else {
            dockerStatus.setHealth(DockerStatus.HEALTH_MIN);
        }
        dockerStatus.setSslRpcPort(baseConfiguration.getSslRpcPort());
        dockerStatus.setPublicWsPort(baseConfiguration.getPublicWsPort());
        dockerStatus.setTime(ChatUtils.dateString(System.currentTimeMillis()));
        dockerStatus.setStatus(DockerStatus.STATUS_STARTING);
        Map<String, Object> info = new HashMap<String, Object>();
        dockerStatus.setInfo(info);
        return dockerStatus;
    }

    public void start() {
        try {
            if (dockerStatusService != null) {
                dockerStatus = generateDockerStatus(baseConfiguration.getServerPort());
                try {
                    dockerStatusService.deleteDockerStatus(OnlineServer.getInstance().getIp(), baseConfiguration.getServerType(), baseConfiguration.getDockerName());
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                dockerStatusService.addDockerStatus(dockerStatus);
            }
            if (tasks != null) {
                for (Task task : tasks) {
                    task.setOnlineServer(this);
                    task.init();
                    LoggerEx.info(TAG, "Task " + task + " initialized!");
                    int numOfThreads = task.getNumOfThreads();
                    for (int i = 0; i < numOfThreads; i++) {
                        ServerStart.getInstance().getGatewayThreadPoolExecutor().execute(task);
                    }
                }
            }

        } catch (Throwable e) {
            e.printStackTrace();
            LoggerEx.error(TAG, "Start online server " + baseConfiguration.getServer() + " failed, " + ExceptionUtils.getFullStackTrace(e));
            if (dockerStatusService != null) {
                try {
                    dockerStatusService.deleteDockerStatus(baseConfiguration.getServer());
                    LoggerEx.info(TAG, "Deleted OnlineServer " + baseConfiguration.getServer() + " because of error " + ExceptionUtils.getFullStackTrace(e));
                } catch (CoreException e1) {
                    e.printStackTrace();
                    LoggerEx.info(TAG, "Remove online server " + baseConfiguration.getServer() + " failed, " + ExceptionUtils.getFullStackTrace(e1));
                }
            }
            OnlineServer.shutdownNow();
            System.exit(0);
        }
    }
    public static void shutdownNow() {
        OnlineServer onlineServer = OnlineServer.getInstance();
        if (onlineServer != null)
            onlineServer.shutdown();
    }

    public void shutdown() {
        LoggerEx.info(TAG, "OnlineServer " + baseConfiguration.getServer() + " is shutting down");
        if (startHandler != null) {
            try {
                startHandler.serverWillShutdown(this);
            } catch (Exception e) {
                e.printStackTrace();
                LoggerEx.fatal(TAG, "StartHandler " + startHandler + " shutdown failed, " + ExceptionUtils.getFullStackTrace(e));
            }
        }
        if (tasks != null) {
            LoggerEx.info(TAG, "Deleted tasks " + tasks + " size " + tasks.size());
            for (Task task : tasks) {
                try {
                    LoggerEx.info(TAG, "Task " + task + " is shutting down");
                    task.shutdown();
                    LoggerEx.info(TAG, "Task " + task + " has been shutdown");
                } catch (Exception e) {
                    e.printStackTrace();
                    LoggerEx.fatal(TAG, "Task " + task + " shutdown failed, " + ExceptionUtils.getFullStackTrace(e));
                }
            }
        }
        CacheStorageFactory.getInstance().releaseAllCacheStorageAdapter(CacheStorageMethod.METHOD_REDIS);
        MongoClientFactory.getInstance().releaseAllMongoClient();
//        if (shutdownList != null) {
//            LoggerEx.info(TAG, "Deleted shutdownListener " + shutdownList + " size " + shutdownList.size());
//            for (ShutdownListener shutdownListener : shutdownList) {
//                try {
//                    LoggerEx.info(TAG, "shutdownListener " + shutdownListener + " is shutting down");
//                    shutdownListener.shutdown();
//                    LoggerEx.info(TAG, "shutdownListener " + shutdownListener + " has been shutdown");
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    LoggerEx.fatal(TAG, "shutdownListener " + shutdownListener + " shutdown failed, " + e.getMessage());
//                }
//            }
//        }
    }
}