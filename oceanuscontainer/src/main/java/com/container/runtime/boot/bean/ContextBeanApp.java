package com.container.runtime.boot.bean;

import chat.config.BaseConfiguration;
import chat.utils.IPHolder;
import com.container.runtime.boot.manager.BootManager;
import com.container.runtime.executor.DefaultRuntimeExecutor;
import com.docker.context.impl.DefaultContextFactory;
import com.docker.http.MyHttpParameters;
import com.docker.server.OnlineServer;
import oceanus.sdk.rpc.impl.RMIServerHandler;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import script.core.runtime.classloader.impl.DefaultClassLoaderFactory;
import script.core.runtime.impl.DefaultRuntimeFactory;
import script.core.servlets.RequestPermissionHandler;
import script.file.FileAdapter;
import script.file.LocalFileHandler;
import script.filter.JsonFilterFactory;

import java.util.HashMap;
import java.util.Map;

//import com.dobybros.chat.log.LogIndexQueue;
//import com.dobybros.chat.storage.mongodb.daos.BulkLogDAO;

/**
 * @Auther: lick
 * @Description:
 * @Date:2019/5/26 15:41
 */

class ContextBeanApp {
    private static final String TAG = ContextBeanApp.class.getSimpleName();
    protected static BaseConfiguration baseConfiguration;
    private PlainSocketFactory plainSocketFactory;
    private SSLSocketFactory sslSocketFactory;
    private DefaultHttpClient httpClient;
    private Scheme httpScheme;
    private Scheme httpsScheme;
    private SchemeRegistry schemeRegistry;
    private ThreadSafeClientConnManager clientConnManager;

    private FileAdapter fileAdapter;
    private IPHolder ipHolder;
    private JsonFilterFactory jsonFilterFactory;
    private RequestPermissionHandler requestPermissionHandler;
    private BootManager scriptManager;
    private OnlineServer onlineServer;
    private RMIServerHandler dockerRpcServerAdapter;

//    private KafkaSimplexListener queueSimplexListener;
//
//    synchronized QueueSimplexListener getQueueSimplexListener() {
//        if (queueSimplexListener == null) {
//            try {
//                queueSimplexListener = new KafkaSimplexListener();
//                Map<String, String> config = new HashMap<>();
////                config.put("bootstrap.servers", getKafkaServers());
////                config.put("producer.key.serializer", getKafkaProducerKeySerializer());
////                config.put("producer.value.serializer", getKafkaProducerValueSerializer());
////                config.put("retries", getKafkaProducerRetries());
////                config.put("linger.ms", getKafkaProducerLingerMs());
////                config.put("consumer.key.serializer", getKafkaConsumerKeySerializer());
////                config.put("consumer.value.serializer", getKafkaConsumerValueSerializer());
//                queueSimplexListener.setConfig(config);
//                queueSimplexListener.setDockerRpcServer(getDockerRpcServer());
//            } catch (Throwable t) {
//                t.printStackTrace();
//            }
//        }
//        return queueSimplexListener;
//    }

    private DefaultRuntimeFactory runtimeFactory;
    synchronized DefaultRuntimeFactory getRuntimeFactory(){
        if(runtimeFactory == null){
            runtimeFactory = new DefaultRuntimeFactory();
        }
        return runtimeFactory;
    }
    private DefaultClassLoaderFactory classLoaderFactory;
    DefaultClassLoaderFactory getClassLoaderFactory(){
        if(classLoaderFactory == null){
            classLoaderFactory = new DefaultClassLoaderFactory();
        }
        return classLoaderFactory;
    }

    BaseConfiguration getBaseConfiguration(){
        return baseConfiguration;
    }
    private DefaultContextFactory contextFactory;
    synchronized DefaultContextFactory getContextFactory(){
        if(contextFactory == null){
            contextFactory = new DefaultContextFactory();
        }
        return contextFactory;
    }
    synchronized OnlineServer getOnlineServer() {
        if (onlineServer == null) {
            onlineServer = new OnlineServer();
//            onlineServer.setDockerStatusService(getDockerStatusService());
            onlineServer.setIpHolder(getIpHolder());
        }
        return onlineServer;
    }

    synchronized BootManager getBootManager() {
        if (scriptManager == null) {
            scriptManager = new BootManager();
            scriptManager.setRuntimeExecutor(new DefaultRuntimeExecutor());
        }
        return scriptManager;
    }

    synchronized RequestPermissionHandler getRequestPermissionHandler() {
        if (requestPermissionHandler == null) {
            requestPermissionHandler = new RequestPermissionHandler();
        }
        return requestPermissionHandler;
    }

    synchronized JsonFilterFactory getJsonFilterFactory() {
        if (jsonFilterFactory == null) {
            jsonFilterFactory = new JsonFilterFactory();
        }
        return jsonFilterFactory;
    }

    synchronized IPHolder getIpHolder() {
        if (ipHolder == null) {
            ipHolder = new IPHolder();
            ipHolder.setEthPrefix(baseConfiguration.getEthPrefix());
            ipHolder.setIpPrefix(baseConfiguration.getIpPrefix());
        }
        return ipHolder;
    }

    synchronized PlainSocketFactory getPlainSocketFactory() {
        if (plainSocketFactory == null) {
            plainSocketFactory = PlainSocketFactory.getSocketFactory();
        }
        return plainSocketFactory;
    }

    synchronized SSLSocketFactory getSslSocketFactory() {
        if (sslSocketFactory == null) {
            sslSocketFactory = SSLSocketFactory.getSocketFactory();
        }
        return sslSocketFactory;
    }

    synchronized Scheme getHttpScheme() {
        if (httpScheme == null) {
            httpScheme = new Scheme("http", 80, getPlainSocketFactory());
        }
        return httpScheme;
    }

    synchronized Scheme getHttpsScheme() {
        if (httpsScheme == null) {
            httpsScheme = new Scheme("https", 443, getSslSocketFactory());
        }
        return httpsScheme;
    }

    synchronized DefaultHttpClient getHttpClient() {
        if (httpClient == null) {
            MyHttpParameters myHttpParameters = new MyHttpParameters();
            myHttpParameters.setCharset("utf8");
            myHttpParameters.setConnectionTimeout(30000);
            myHttpParameters.setSocketTimeout(30000);
            httpClient = new DefaultHttpClient(getClientConnManager(), myHttpParameters);
        }
        return httpClient;
    }

    synchronized SchemeRegistry getSchemeRegistry() {
        if (schemeRegistry == null) {
            schemeRegistry = new SchemeRegistry();
            Map map = new HashMap();
            map.put("http", getHttpScheme());
            map.put("https", getHttpsScheme());
            schemeRegistry.setItems(map);
        }
        return schemeRegistry;
    }

    synchronized ThreadSafeClientConnManager getClientConnManager() {
        if (clientConnManager == null) {
            clientConnManager = new ThreadSafeClientConnManager(getSchemeRegistry());
            clientConnManager.setMaxTotal(20);
        }
        return clientConnManager;
    }

    synchronized FileAdapter getFileAdapter() {
        if (fileAdapter == null) {
            fileAdapter = new LocalFileHandler();
            ((LocalFileHandler)fileAdapter).setRootPath(baseConfiguration.getRemotePath());
        }
        return fileAdapter;
    }
//    static ContextBeanApp getInstance() {
//        if (instance == null) {
//            synchronized (ContextBeanApp.class) {
//                if (instance == null) {
//                    instance = new ContextBeanApp();
//                    baseConfiguration = new BaseConfigurationBuilder().build();
//                    BeanFactory.init(baseConfiguration);
//                }
//            }
//        }
//        return instance;
//    }
}
