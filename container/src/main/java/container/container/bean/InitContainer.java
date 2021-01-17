package container.container.bean;

import chat.utils.IPHolder;
import com.alibaba.fastjson.util.TypeUtils;
import com.docker.file.adapters.GridFSFileHandler;
import com.docker.onlineserver.OnlineServerWithStatus;
import com.docker.rpc.impl.RMIServerHandler;
import com.docker.storage.mongodb.MongoHelper;
import com.docker.storage.mongodb.daos.*;
import com.proxy.runtime.ScriptManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import com.docker.utils.BeanFactory;

/**
 * Created by lick on 2019/5/27.
 * Description：
 */
@Component
public class InitContainer implements CommandLineRunner{
    @Autowired
    MongoHelper dockerStatusHelper;
    @Autowired
    MongoHelper configHelper;
    @Autowired
    MongoHelper scheduledTaskHelper;
    @Autowired
    DockerStatusDAO dockerStatusDAO;
    @Autowired
    ServiceVersionDAO serviceVersionDAO;
    @Autowired
    DeployServiceVersionDAO deployServiceVersionDAO;
    @Autowired
    ServersDAO serversDAO;
    @Autowired
    ScheduledTaskDAO scheduledTaskDAO;
    @Autowired
    LansDAO lansDAO;
    @Autowired
    SDockerDAO sDockerDAO;
    @Autowired
    MongoHelper repairHelper;
    @Autowired
    RepairDAO repairDAO;
    @Autowired
    MongoHelper logsHelper;
    @Autowired
    MongoHelper gridfsHelper;
    @Autowired
    GridFSFileHandler fileAdapter;
    @Autowired
    IPHolder ipHolder;
    @Autowired
    ScriptManager scriptManager;
    @Autowired
    RMIServerHandler dockerRpcServerAdapter;
    @Autowired
    RMIServerHandler dockerRpcServerAdapterSsl;
    @Autowired
    OnlineServerWithStatus onlineServer;

    @Override
    public void run(String... args) throws Exception {
        BeanFactory.map();
        TypeUtils.compatibleWithJavaBean = true;
        System.setProperty("es.set.netty.runtime.available.processors", "false");
        dockerStatusHelper.init();
        configHelper.init();
        scheduledTaskHelper.init();
        dockerStatusDAO.init();
        serviceVersionDAO.init();
        deployServiceVersionDAO.init();
        serversDAO.init();
        lansDAO.init();
        sDockerDAO.init();
        repairHelper.init();
        repairDAO.init();
        scheduledTaskDAO.init();
        logsHelper.init();
        gridfsHelper.init();
        fileAdapter.init();
        ipHolder.init();
        onlineServer.start();
        scriptManager.init();
        dockerRpcServerAdapter.serverStart();
        dockerRpcServerAdapterSsl.serverStart();
    }
}
