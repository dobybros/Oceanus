package com.container.runtime.boot.bean;

import chat.base.bean.annotation.OceanusBean;
import com.docker.file.adapters.GridFSFileHandler;
import com.docker.storage.adapters.impl.*;
import com.docker.storage.mongodb.MongoHelper;
import com.docker.storage.mongodb.daos.*;
import com.docker.storage.redis.RedisListenerHandler;
import com.docker.storage.redis.RedisSubscribeHandler;
import com.docker.storage.zookeeper.ZookeeperFactory;
import script.file.FileAdapter;

/**
 * @Auther: lick
 * @Description:
 * @Date:2019/5/26 16:59
 */
@OceanusBean
public class DatabaseBean {
    private BeanApp instance;

    public DatabaseBean() {
        instance = BeanApp.getInstance();
    }

//    @OceanusBean
//    public MongoHelper dockerStatusHelper() {
//        return instance.getDockerStatusHelper();
//    }

    //    @Bean(initMethod = "init", destroyMethod = "disconnect")
//    @OceanusBean
//    public MongoHelper logsHelper() {
//        return instance.getLogsHelper();
//    }

    //    @Bean(initMethod = "init")
//    @OceanusBean
//    public MongoHelper configHelper() {
//        return instance.getConfigHelper();
//    }

    //    @Bean(initMethod = "init")
//    @OceanusBean
//    public DockerStatusDAO dockerStatusDAO() {
//        return instance.getDockerStatusDAO();
//    }

//    @OceanusBean
//    public ServiceVersionDAO serviceVersionDAO() {
//        return instance.getServiceVersionDAO();
//    }

//    @OceanusBean
//    public DeployServiceVersionDAO deployServiceVersionDAO() {
//        return instance.getDeployServiceVersionDAO();
//    }

    //    @Bean(initMethod = "init")
//    @OceanusBean
//    public ServersDAO serversDAO() {
//        return instance.getServersDAO();
//    }

    //    @Bean(initMethod = "init")
//    @OceanusBean
//    public LansDAO lansDAO() {
//        return instance.getLansDAO();
//    }

//    @OceanusBean
//    public DockerStatusServiceImpl dockerStatusService() {
//        return instance.getDockerStatusService();
//    }

//    @OceanusBean
//    public ServersServiceImpl serversService() {
//        return instance.getServersService();
//    }

//    @OceanusBean
//    public LansServiceImpl lansService() {
//        return instance.getLansService();
//    }

//    @OceanusBean
//    public SDockersServiceImpl sdockersService() {
//        return new SDockersServiceImpl();
//    }

    //    @Bean(initMethod = "init")
//    @OceanusBean
//    public SDockerDAO sdockerDAO() {
//        return instance.getSdockerDAO();
//    }

//   @OceanusBean
//    public MongoHelper gridfsHelper() {
//        return instance.getGridfsHelper();
//    }

    //    @Bean(initMethod = "init")
    @OceanusBean
    public FileAdapter fileAdapter() {
        return instance.getFileAdapter();
    }

//    @OceanusBean
//    public ServiceVersionServiceImpl serviceVersionService() {
//        return instance.getServiceVersionService();
//    }
//
//    @OceanusBean
//    public ScheduledTaskServiceImpl scheduledTaskService() {
//        return instance.getScheduledTaskService();
//    }
//
//    @OceanusBean
//    public DeployServiceVersionServiceImpl deployServiceVersionService() {
//        return instance.getDeployServiceVersionService();
//    }
//
//    @OceanusBean
//    public ScheduledTaskDAO scheduledTaskDAO() {
//        return instance.getScheduledTaskDAO();
//    }
//
//    @OceanusBean
//    public MongoHelper scheduledTaskHelper() {
//        return instance.getScheduledTaskHelper();
//    }
//
//    @OceanusBean
//    public RedisSubscribeHandler redisSubscribeHandler() {
//        return instance.getRedisSubscribeHandler();
//    }
//
//    @OceanusBean
//    public MongoHelper repairHelper() {
//        return instance.getRepairHelper();
//    }
//
//    @OceanusBean
//    public RepairDAO repairDAO() {
//        return instance.getRepairDAO();
//    }
//
//    @OceanusBean
//    public RepairServiceImpl repairService() {
//        return instance.getRepairService();
//    }
//
//    @OceanusBean
//    public RedisListenerHandler redisListenerHandler() {
//        return instance.getRedisListenerHandler();
//    }
//
//    @OceanusBean
//    public ZookeeperFactory zkFactory() {
//        return instance.getZookeeperFactory();
//    }
}
