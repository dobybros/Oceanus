package com.container.runtime.boot;

import ch.qos.logback.classic.Level;
import chat.config.BaseConfiguration;
import chat.errors.CoreException;
import chat.logs.LoggerEx;
import chat.utils.IPHolder;
import com.container.runtime.boot.handler.OceanusBeanAnnotationHandler;
import com.container.runtime.boot.manager.BootManager;
import com.container.runtime.boot.manager.DefaultOceansBeanManager;
import com.docker.oceansbean.BeanFactory;
import com.docker.oceansbean.OceanusBeanManager;
import com.docker.onlineserver.OnlineServerWithStatus;
import com.docker.rpc.impl.RMIServerHandler;
import com.docker.script.GroovyServletScriptDispatcher;
import org.apache.commons.cli.*;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import script.core.servlets.GroovyServletDispatcher;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Created by lick on 2021/1/2.
 * Description：
 */
public class OceanusStart {
    private static String TAG = OceanusStart.class.getSimpleName();
    public static void main(String[] args){
        try {
            handleArgs(args);

            prepareLog();
            buildOceanusBeanManager();
            BaseConfiguration baseConfiguration = (BaseConfiguration) BeanFactory.getBean(BaseConfiguration.class.getName());
            Server server = new Server(new QueuedThreadPool(baseConfiguration.getHttpThreadPoolSize()));
            ServerConnector serverConnector = new ServerConnector(server);
            Assert.assertNotNull(baseConfiguration);
            serverConnector.setPort(baseConfiguration.getServerPort());
            server.addConnector(serverConnector);
            ServletHandler servletHandler = new ServletHandler();
            server.setHandler(servletHandler);
            servletHandler.addServletWithMapping(GroovyServletDispatcher.class, "/rest/*");
            servletHandler.addServletWithMapping(GroovyServletScriptDispatcher.class, "/base/*");
            server.start();
            init();
            LoggerEx.info(TAG, "Server started on port " + baseConfiguration.getServerPort());
            server.join();
        }catch (Exception e){
            LoggerEx.error(TAG, "Start server failed, errMsg: " + ExceptionUtils.getFullStackTrace(e));
            System.exit(1);
        }
    }

    private static void handleArgs(String[] args) throws ParseException {
        CommandLineParser parser = new PosixParser();
        Options opt = new Options();
        opt.addOption("h", "help", false, "help")
                .addOption("p", true, "Oceanus properties path")
//			.addOption("a",true, "async servlet map")
                .addOption("t", true, "Http Server thread pool size")
                ;

        org.apache.commons.cli.CommandLine line = parser.parse(opt, args);
        System.out.println("commandLine " + Arrays.toString(args));
        List<String> argList = line.getArgList();
        if (line.hasOption('h') || line.hasOption("help")) {
            HelpFormatter hf = new HelpFormatter();
            hf.printHelp("OceanusStart[options:]", opt, false);
            return;
        }

        if (line.hasOption('p')) {
            String oceanusConfigPath = line.getOptionValue('p');
            BaseConfiguration.setOceanusConfigPath(oceanusConfigPath);
        }
        if (line.hasOption('t')) {
            String threadCountStr = line.getOptionValue('t');
            try {
                Integer threadPoolSize = Integer.valueOf(threadCountStr);
                BaseConfiguration.setHttpThreadPoolSize(threadPoolSize);
            } catch (Exception e) {
            }
        }
    }

    private static void prepareLog(){
        Logger logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        ((ch.qos.logback.classic.Logger) logger).setLevel(Level.INFO);
    }

    private static void buildOceanusBeanManager() throws CoreException {
        OceanusBeanManager oceanusBeanManager = new DefaultOceansBeanManager();
        BeanFactory.setOceanusBeanManager(oceanusBeanManager);
        new OceanusBeanAnnotationHandler(oceanusBeanManager).handle(null);
    }

    private static void init() throws IOException {
        IPHolder ipHolder = (IPHolder) BeanFactory.getBeanByName("ipHolder");
        ipHolder.init();
        OnlineServerWithStatus onlineServer = (OnlineServerWithStatus) BeanFactory.getBeanByName("onlineServer");
        onlineServer.start();
        BootManager bootManager = (BootManager) BeanFactory.getBeanByName("bootManager");
        bootManager.init();
        RMIServerHandler dockerRpcServerAdapter = (RMIServerHandler) BeanFactory.getBeanByName("dockerRpcServerAdapter");
        dockerRpcServerAdapter.serverStart();
        RMIServerHandler dockerRpcServerAdapterSsl = (RMIServerHandler) BeanFactory.getBeanByName("dockerRpcServerAdapterSsl");
        dockerRpcServerAdapterSsl.serverStart();
    }
}
