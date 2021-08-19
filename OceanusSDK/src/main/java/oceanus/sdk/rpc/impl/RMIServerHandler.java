package oceanus.sdk.rpc.impl;

import oceanus.sdk.core.discovery.errors.CoreErrorCodes;
import oceanus.apis.CoreException;
import oceanus.sdk.logger.LoggerEx;
import oceanus.sdk.rpc.RPCRequest;
import oceanus.sdk.rpc.RPCResponse;
import oceanus.sdk.rpc.RPCServerAdapter;
import oceanus.sdk.server.OnlineServer;
import oceanus.sdk.utils.IPHolder;
import oceanus.sdk.utils.OceanusProperties;
import org.apache.commons.lang3.StringUtils;

import java.net.InetAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.ConcurrentHashMap;

public class RMIServerHandler {
    public static final int RMI_PORT = 2222;

    private String rmiId;
    private Integer rmiPort = RMI_PORT;
    private String serverName;

//    @Resource
    private IPHolder ipHolder;
    private ConcurrentHashMap<String, RPCEntity> typeEntityMap = new ConcurrentHashMap<>();

    //both
    private Registry registry;
    private RMIServer server;

    private boolean enableSsl = false;

    private static final String TAG = "RMI";

    private boolean isStarted = true;

    /** rpc ssl certificate */
    private String rpcSslClientTrustJksPath;
    private String rpcSslServerJksPath;
    private String rpcSslJksPwd;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(RMIServerHandler.class.getSimpleName() + ": ");
        builder.append("rmiId: " + rmiId + " ");
        builder.append("rmiPort: " + rmiPort + " ");
        builder.append("server: " + server + " ");
        builder.append("isStarted: " + isStarted + " ");
        return builder.toString();
    }

    public synchronized void serverStart() {
        serverStart(ipHolder.getIp());
    }

    public synchronized void serverStart(String ip) {
        rmiId = OnlineServer.getInstance().getServer();
        try {
//            if(enableSsl && !rmiId.endsWith(RMIID_SSL_SUFFIX))
//                rmiId = rmiId + RMIID_SSL_SUFFIX;
            LoggerEx.info(TAG, "InetAddress host name : " + InetAddress.getLocalHost().getHostName() + ", InetAddress host address : " + InetAddress.getLocalHost().getHostAddress());
            System.setProperty("java.rmi.server.hostname", ip);
            System.setProperty("java.rmi.server.port", String.valueOf(rmiPort)); //I made it up for pass port to somewhere else.
            registry = LocateRegistry.createRegistry(rmiPort);
            server = new RMIServerImpl(OceanusProperties.getInstance().getRpcDataPort());

//            registry = LocateRegistry.createRegistry(rmiPort);
//            server = serverImpl.initServer();

            registry.bind(rmiId, server);
            LoggerEx.info(TAG, "RMI server IP : " + ipHolder.getIp() + " port : " + rmiPort + " rmiId " + rmiId + " started!" + " System host name : " + System.getProperty("java.rmi.server.hostname") + ", System port : " + ipHolder.getIp());
        } catch(Throwable t) {
            t.printStackTrace();
            LoggerEx.fatal(TAG, "RMIClientHandler server start failed. Server will be shutdown... " + t.getMessage());
            OnlineServer.shutdownNow();
            System.exit(0);
        }
    }

    public synchronized void serverDestroy() {
        try {
            registry.unbind(rmiId);
            LoggerEx.info(TAG, "RMI port " + rmiPort + " server " + serverName + " server is destroyed!");
        } catch (Throwable e) {
            e.printStackTrace();
            LoggerEx.error(TAG, "RMI port " + rmiPort + " server " + serverName + " server destroy failed, " + e.getMessage());
        }
    }

    // todo 需要修改
    public RPCEntity getRPCEntityForServer(String requestType, Class<RPCServerAdapter> serverAdapterClass) throws CoreException {
        if(StringUtils.isBlank(requestType)) return null;

        RPCEntity entity = typeEntityMap.get(requestType);
        if(entity == null) {
            Class<? extends RPCRequest> requestClass = null;
            Class<? extends RPCResponse> responseClass = null;
            switch (requestType) {
                case "smsg":
                    try {
                        requestClass = (Class<? extends RPCRequest>) Class.forName("com.dobybros.chat.rpc.reqres.balancer.ServerMessageRequest");
                        responseClass = (Class<? extends RPCResponse>) Class.forName("com.dobybros.chat.rpc.reqres.balancer.ServerMessageResponse");
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                    break;
                case "uol":
                    try {
                        requestClass = (Class<? extends RPCRequest>) Class.forName("com.dobybros.chat.rpc.reqres.balancer.UserOnlineRequest");
                        responseClass = (Class<? extends RPCResponse>) Class.forName("com.dobybros.chat.rpc.reqres.balancer.UserOnlineResponse");
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                    break;
                case "proxyim":
                    try {
                        requestClass = (Class<? extends RPCRequest>) Class.forName("com.dobybros.chat.rpc.reqres.balancer.ProxyIMRequest");
                        responseClass = (Class<? extends RPCResponse>) Class.forName("com.dobybros.chat.rpc.reqres.balancer.ProxyIMResponse");
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                    break;
                case "improxy":
                    try {
                        requestClass = (Class<? extends RPCRequest>) Class.forName("com.dobybros.chat.rpc.reqres.balancer.IMProxyRequest");
                        responseClass = (Class<? extends RPCResponse>) Class.forName("com.dobybros.chat.rpc.reqres.balancer.IMProxyResponse");
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                    break;
            }
            if(requestClass != null && responseClass != null) {
                entity = new RPCEntity();
                entity.requestClass = requestClass;
                entity.responseClass = responseClass;
            } else {
                throw new CoreException(CoreErrorCodes.ERROR_RPC_ILLEGAL, "RequestClass " + requestClass + " and ResponseClass " + responseClass + " is not prepared for requestType " + requestType);
            }
            RPCEntity previousEntity = typeEntityMap.putIfAbsent(requestType, entity);
            if(previousEntity != null)
                entity = previousEntity;
        }
        return entity;
    }
//    RPCEntity getRPCEntityForServer(String requestType, Class<RPCServerAdapter> serverAdapterClass) throws CoreException {
//        RPCEntity entity = typeEntityMap.get(requestType);
//        if(entity == null) {
//            Class<? extends RPCRequest> requestClass = null;
//            Class<? extends RPCResponse> responseClass = null;
//            Type[] types = serverAdapterClass.getGenericInterfaces();
//            for (Type type : types) {
//                if(type instanceof ParameterizedType) {
//                    ParameterizedType pType = (ParameterizedType) type;
//                    if(pType.getRawType().equals(RPCServerAdapter.class)) {
//                        Type[] params = pType.getActualTypeArguments();
//                        if(params != null && params.length == 2) {
//                            requestClass = (Class<? extends RPCRequest>) params[0];
//                            responseClass = (Class<? extends RPCResponse>) params[1];
//                        }
//                    }
//                }
//            }
//
//            if(requestClass != null && responseClass != null) {
//                entity = new RPCEntity();
//                entity.requestClass = requestClass;
//                entity.responseClass = responseClass;
//            } else {
//                throw new CoreException(CoreErrorCodes.ERROR_RPC_ILLEGAL, "RequestClass " + requestClass + " and ResponseClass " + responseClass + " is not prepared for requestType " + requestType);
//            }
//            RPCEntity previousEntity = typeEntityMap.putIfAbsent(requestType, entity);
//            if(previousEntity != null)
//                entity = previousEntity;
//        }
//        return entity;
//    }
    public String getRmiId() {
        return rmiId;
    }
    // public void setRmiId(String rmiId) {
//    this.rmiId = rmiId;
// }
    public Integer getRmiPort() {
        return rmiPort;
    }

    public void setRmiPort(Integer rmiPort) {
        this.rmiPort = rmiPort;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public boolean isEnableSsl() {
        return enableSsl;
    }

    public void setEnableSsl(boolean enableSsl) {
        this.enableSsl = enableSsl;
    }

    public IPHolder getIpHolder() {
        return ipHolder;
    }

    public void setIpHolder(IPHolder ipHolder) {
        this.ipHolder = ipHolder;
    }
}