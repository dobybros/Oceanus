package container.container.bean;

import chat.utils.IPHolder;
import com.alibaba.fastjson.util.TypeUtils;
import com.dobybros.chat.handlers.ProxyContainerDuplexSender;
import com.dobybros.chat.handlers.imextention.IMExtensionCache;
import com.dobybros.gateway.onlineusers.impl.OnlineUserManagerImpl;
import com.docker.file.adapters.GridFSFileHandler;
import com.docker.onlineserver.OnlineServerWithStatus;
import com.docker.rpc.impl.RMIServerHandler;
import com.docker.storage.mongodb.MongoHelper;
import com.docker.storage.mongodb.daos.*;
import com.container.runtime.BootManager;
import org.apache.mina.transport.socket.nio.NioSocketAcceptorEx;
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
    NioSocketAcceptorEx tcpIoAcceptor;
    @Autowired
    NioSocketAcceptorEx wsIoAcceptor;
    @Autowired
    IPHolder ipHolder;
    @Autowired
    BootManager scriptManager;
    @Autowired
    RMIServerHandler dockerRpcServerAdapter;
    @Autowired
    RMIServerHandler dockerRpcServerAdapterSsl;
    @Autowired
    OnlineServerWithStatus onlineServer;
    @Autowired
    OnlineUserManagerImpl onlineUserManager;
    @Autowired
    ProxyContainerDuplexSender proxyContainerDuplexSender;
    @Autowired
    IMExtensionCache imExtensionCache;

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
        tcpIoAcceptor.bind();
        wsIoAcceptor.bind();
        ipHolder.init();
        onlineServer.start();
        onlineUserManager.init();
        scriptManager.init();
        dockerRpcServerAdapter.serverStart();
        dockerRpcServerAdapterSsl.serverStart();
        proxyContainerDuplexSender.init();
        imExtensionCache.init();
    }
}
