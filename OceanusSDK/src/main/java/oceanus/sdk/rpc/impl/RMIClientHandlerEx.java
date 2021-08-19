package oceanus.sdk.rpc.impl;

import oceanus.apis.CoreException;
import oceanus.sdk.core.net.NetRuntime;
import oceanus.sdk.errors.ChatErrorCodes;
import oceanus.sdk.logger.LoggerEx;
import oceanus.sdk.rpc.*;
import oceanus.sdk.rpc.remote.stub.RpcCacheManager;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.SocketTimeoutException;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class RMIClientHandlerEx extends RPCClientAdapter {
    public static final int RMI_PORT = 2222;

    private String rmiId;
    private Integer rmiPort = RMI_PORT;
    //both
    private Registry registry;
    private RMIServer server;

    private final ConcurrentHashMap<String, RPCEntity> typeEntityMap = new ConcurrentHashMap<>();

    //Client
    private String serverHost;

    private Long touch;
    private final Long idleCheckPeriod = TimeUnit.SECONDS.toMillis(15);
    private static final String TAG = "RMIClientHandler";
    private Long expireTime;
    private ExpireListener<RPCClientAdapter> expireListener;
    private DisconnectedAfterRetryListener disconnectedAfterRetryListener;

    public static final int STATUS_IDLE = 0;
    public static final int STATUS_CONNECTING = 10;
    public static final int STATUS_CONNECTED = 20;
    public static final int STATUS_DISCONNECTED = 30;
    public static final int STATUS_TERMINATED = 100;
    private final AtomicInteger status = new AtomicInteger(STATUS_IDLE);
    private ConnectHandler connectHandler;

    @Override
    public String toString() {
        //        builder.append("averageCounter: " + averageCounter + " ");
        String builder = RMIClientHandlerEx.class.getSimpleName() + ": " + "rmiId: " + rmiId + " " +
                "rmiPort: " + rmiPort + " " +
                "server: " + server + " " +
                "status: " + status + " ";
        return builder;
    }

    public void setExpireTime(Long expireTime, ExpireListener<RPCClientAdapter> expireListener) {
        this.expireTime = expireTime;
        this.expireListener = expireListener;
    }

    public void touch() {
        touch = System.currentTimeMillis();
    }

    public class ConnectHandler {
        private int connectCountDown = 10;
        private ScheduledFuture<?> healthCheckFuture, retryFuture;
        private AtomicBoolean jobDone = new AtomicBoolean(false);

        public void connect() {
            if(jobDone.get()) {
                LoggerEx.error(TAG, "ConnectHandler is done already, status " + status.get());
                return;
            }
            try {
                registry = LocateRegistry.getRegistry(serverHost, rmiPort);
                server = (RMIServer) registry.lookup(rmiId);

                LoggerEx.info(TAG, "RMI " + serverHost + " port " + rmiPort + " server " + rmiId + " client connected!");
                if(jobDone.compareAndSet(false, true)) {
                    if(!status.compareAndSet(STATUS_CONNECTING, STATUS_CONNECTED)) {
                        LoggerEx.error(TAG, "changed from STATUS_CONNECTING to STATUS_CONNECTED failed, current is " + status.get());
                    } else {
                        connectHandler = null;
                        if(healthCheckFuture != null) {
                            healthCheckFuture.cancel(true);
                        }
                        long healthCheckPeriodSeconds = 20L;
                        healthCheckFuture = NetRuntime.getScheduledExecutorService().scheduleAtFixedRate(this::healthCheck, healthCheckPeriodSeconds, healthCheckPeriodSeconds, TimeUnit.SECONDS);
                    }
                } else {
                    LoggerEx.error(TAG, "ConnectHandler is done already, status " + status.get());
                }
            } catch (Throwable t) {
                t.printStackTrace();
                LoggerEx.error(TAG, "RMI clientStart failed, " + t.getMessage() + " connectCountDown " + connectCountDown);
                if(--connectCountDown > 0) {
                    if(retryFuture != null) {
                        retryFuture.cancel(true);
                    }
                    long delaySeconds = 3L;
                    retryFuture = NetRuntime.getScheduledExecutorService().schedule(this::retry, delaySeconds, TimeUnit.SECONDS);
                } else {
                    if(!status.compareAndSet(STATUS_CONNECTING, STATUS_DISCONNECTED)) {
                        LoggerEx.error(TAG, "changed from STATUS_CONNECTING to STATUS_DISCONNECTED failed, current is " + status.get());
                    } else {
                        connectHandler.shutdown();
                        connectHandler = null;
                        if(disconnectedAfterRetryListener != null) {
                            try {
                                disconnectedAfterRetryListener.disconected(RMIClientHandlerEx.this);
                            } catch(Throwable e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }

        private void shutdown() {
            if(healthCheckFuture != null) {
                healthCheckFuture.cancel(true);
            }
            if(retryFuture != null) {
                retryFuture.cancel(true);
            }
        }

        private void healthCheck() {
            if (expireTime != null && expireListener != null) {
                if (touch + expireTime < System.currentTimeMillis()) {
                    try {
                        expireListener.expired(RMIClientHandlerEx.this, touch, expireTime);
//                        LoggerEx.info(TAG, "Client adapter expired after idle " + (expireTime / 1000) + " seconds." + " server : " + server.toString() + ", serverHost : " + serverHost + " port " + rmiPort);
                    } catch (Throwable t) {
                        t.printStackTrace();
                        LoggerEx.info(TAG, "Handle server expire failed, " + t.getMessage() + " the expireListener " + expireListener + " will be ignored..." + " server : " + server.toString() + ", serverHost : " + serverHost + " port " + rmiPort);
                    }
                    return;
                }
            }
            if (touch + idleCheckPeriod < System.currentTimeMillis()) {
                try {
                    server.alive();
                } catch (Throwable ce) {
                    LoggerEx.info(TAG, "Check server alive failed, " + ce.getMessage() + " need reconnect..." + ", server : " + server.toString() + ", serverHost : " + serverHost + " port " + rmiPort);
                    if(!status.compareAndSet(STATUS_CONNECTED, STATUS_CONNECTING)) {
                        LoggerEx.error(TAG, "changed from STATUS_CONNECTED to STATUS_CONNECTING failed, current is " + status.get());
                    } else {
                        handleDisconnect();
                    }
                }
            }
        }

        private void retry() {
            if(status.get() == STATUS_CONNECTING) {
                connect();
            }
        }
    }

    private void startConnectHandler() {
        if(connectHandler != null) {
            connectHandler.shutdown();
        }
        connectHandler = new ConnectHandler();
        connectHandler.connect();
    }

    @Override
    public void clientStart() throws CoreException {
        if(status.compareAndSet(STATUS_IDLE, STATUS_CONNECTING)) {
            for (ClientAdapterStatusListener statusListener : statusListeners) {
                try {
                    statusListener.started(rmiId);
                } catch (Throwable t) {
                    LoggerEx.error(TAG, "statusListener " + statusListener + " started failed, " + t.getMessage() + " server : " + server.toString() + ", serverHost : " + serverHost + " port " + rmiPort);
                }
            }
            if (StringUtils.isBlank(serverHost))
                throw new CoreException(ChatErrorCodes.ERROR_ILLEGAL_PARAMETER, "Server host is illegal, " + serverHost);
            touch();
            startConnectHandler();
        } else {
            LoggerEx.error(TAG, "changed from STATUS_IDLE to STATUS_CONNECTING failed, current is " + status.get() + " server : " + server.toString() + ", serverHost : " + serverHost + " port " + rmiPort);
        }
    }

    @Override
    public void clientDestroy() {
        status.set(STATUS_TERMINATED);
        expireListener = null;
//        if(registry != null) {
//            try {
//                registry.unbind(rmiId);
//            } catch (Throwable ignored) { }
//            registry = null;
//        }
        if (server != null) {
            try {
                boolean bool = UnicastRemoteObject.unexportObject(server, true);
                LoggerEx.info(TAG, "RMI " + serverHost + " port " + rmiPort + " server " + rmiId + " is destroyed, unexport " + server + " result " + bool);
            } catch (Throwable e) {
            }
            server = null;
        }

        LoggerEx.info(TAG, "RMI " + serverHost + " port " + rmiPort + " server " + rmiId + " monitor stopped...");
        for (ClientAdapterStatusListener statusListener : statusListeners) {
            try {
                statusListener.terminated(rmiId);
            } catch (Throwable t) {
                LoggerEx.error(TAG, "statusListener " + statusListener + " terminated failed, " + t.getMessage() + " server : " + server.toString() + ", serverHost : " + serverHost + " port " + rmiPort);
            }
        }
        statusListeners.clear();
    }

    @Override
    public RPCResponse call(RPCRequest request) throws CoreException {
        try {
            RPCResponse response = callPrivate(request);
            for (ClientAdapterStatusListener statusListener : statusListeners) {
                try {
                    statusListener.called(rmiId, request, response);
                } catch (Throwable t) {
                    LoggerEx.error(TAG, "CallListener(called) occured error " + t.getMessage() + " for request " + request + " and response " + response);
                }
            }
            return response;
        } catch (CoreException e) {
            for (ClientAdapterStatusListener statusListener : statusListeners) {
                try {
                    statusListener.callFailed(rmiId, request, e);
                } catch (Throwable t) {
                    LoggerEx.error(TAG, "CallListener(callFailed) occured error " + t.getMessage() + " for request " + request);
                    if (t instanceof CoreException)
                        throw t;
                }
            }
            throw e;
        }
    }

    private RPCResponse callPrivate(RPCRequest request) throws CoreException {
        touch();
        if (status.get() != STATUS_CONNECTED)
            throw new CoreException(ChatErrorCodes.ERROR_RPC_DISCONNECTED, "RPC (" + serverHost + ":" + rmiPort + ") is disconnected for " + request.getType() + ": " + request.toString());
        try {
            handleRequest(request);
//            long time = System.currentTimeMillis();
            byte[] data = server.call(request.getData(), request.getType(), request.getEncode());
//            if (averageCounter != null)
//                averageCounter.add((int) (System.currentTimeMillis() - time));
            if (data == null) {
                return null;
            }
            RPCResponse response = handleResponse(request.getType(), request, data);
            return response;
        } catch (ConnectException | ConnectIOException ce) {
            if (status.compareAndSet(STATUS_CONNECTED, STATUS_CONNECTING)) {
                handleDisconnect();
            } else {
                LoggerEx.error(TAG, "changed from STATUS_CONNECTED to STATUS_CONNECTING failed, current is " + status.get() + " server : " + server.toString() + ", serverHost : " + serverHost + " port " + rmiPort);
            }
            Throwable cause = ce.getCause();
            LoggerEx.error(TAG, "RMI call failed, " + ce.getMessage() + " start reconnecting..." + " server : " + server.toString() + ", serverHost : " + serverHost + " port " + rmiPort);
            if(cause instanceof SocketTimeoutException) {
                throw new CoreException(ChatErrorCodes.ERROR_RMICALL_TIMEOUT, "RMI call timeout, " + ce.getMessage() + " start reconnecting..." + " server : " + server.toString() + ", serverHost : " + serverHost + " port " + rmiPort);
            }
            throw new CoreException(ChatErrorCodes.ERROR_RMICALL_CONNECT_FAILED, "RMI call failed, " + ce.getMessage() + " start reconnecting..." + " server : " + server.toString() + ", serverHost : " + serverHost + " port " + rmiPort);
        } catch (Throwable t) {
            t.printStackTrace();

            CoreException theCoreException = null;
            if(t instanceof UnmarshalException) {
                UnmarshalException unmarshalException = (UnmarshalException) t;
                if(unmarshalException.detail != null)
                    t = unmarshalException.detail;
            }
            if (t instanceof ServerException) {
                Throwable remoteException = t.getCause();
                if (remoteException instanceof RemoteException) {
                    Throwable throwable = remoteException.getCause();
                    if (throwable instanceof CoreException) {
                        CoreException coreException = (CoreException)throwable;
                        if(request instanceof MethodRequest){
                            theCoreException = new CoreException(coreException.getCode(), coreException.getMessage().concat(" $$client: service_class_method: " + RpcCacheManager.getInstance().getMethodByCrc(((MethodRequest)request).getCrc()) + ", fromService: " + ((MethodRequest)request).getFromService()));
                            theCoreException.setParameters(coreException.getParameters());
                            theCoreException.setMoreExceptions(coreException.getMoreExceptions());
                            theCoreException.setData(coreException.getData());
                            theCoreException.setInfoMap(coreException.getInfoMap());
                            theCoreException.setLogLevel(coreException.getLogLevel());
                            coreException = theCoreException;
                        }
                        throw coreException;
                    }
                }
            } else if (t instanceof CoreException){
                CoreException coreException = (CoreException)t;
                if(request instanceof MethodRequest){
                    theCoreException = new CoreException(coreException.getCode(), coreException.getMessage().concat(" $$client: service_class_method: " + RpcCacheManager.getInstance().getMethodByCrc(((MethodRequest)request).getCrc()) + ", fromService: " + ((MethodRequest)request).getFromService()));
                    theCoreException.setParameters(coreException.getParameters());
                    theCoreException.setMoreExceptions(coreException.getMoreExceptions());
                    theCoreException.setData(coreException.getData());
                    theCoreException.setInfoMap(coreException.getInfoMap());
                    theCoreException.setLogLevel(coreException.getLogLevel());
                    coreException = theCoreException;
                }
                throw coreException;
            } else if(t instanceof SocketTimeoutException) {
                throw new CoreException(ChatErrorCodes.ERROR_RMICALL_TIMEOUT, "RMI call timeout, " + t.getMessage() + " start reconnecting...");
            } else {
                Throwable cause = t.getCause();
                if(cause instanceof CoreException) {
                    throw (CoreException)cause;
                } else if(cause instanceof SocketTimeoutException) {
                    throw new CoreException(ChatErrorCodes.ERROR_RMICALL_TIMEOUT, "RMI call timeout, " + cause.getMessage() + " start reconnecting...");
                }
            }

            LoggerEx.error(TAG, "RMI call failed, " + t.getMessage());
            throw new CoreException(ChatErrorCodes.ERROR_RMICALL_FAILED, "RMI call failed, " + t.getMessage());
        }
    }

    private void handleDisconnect() {
        for (ClientAdapterStatusListener statusListener : statusListeners) {
            try {
                statusListener.disconnected(rmiId);
            } catch (Throwable t) {
                LoggerEx.error(TAG, "statusListener " + statusListener + " disconnected failed, " + t.getMessage());
            }
        }
        startConnectHandler();
    }

    RPCEntity getRPCEntityForClient(String requestType, RPCRequest request) throws CoreException {
        RPCEntity entity = typeEntityMap.get(requestType);
        if (entity == null) {
            String requestClass = request.getClass().getName();
            String responseClassString = null;
            final String REQUEST_SUFIX = "Request";
            final String RESPONSE_SUFIX = "Response";
            if (!requestClass.endsWith(REQUEST_SUFIX))
                throw new CoreException(ChatErrorCodes.ERROR_RPC_ILLEGAL, "RequestClass " + requestClass + " don't contain Request as sufix. ");

            responseClassString = requestClass.substring(0, requestClass.length() - REQUEST_SUFIX.length()) + RESPONSE_SUFIX;
            Class<? extends RPCResponse> responseClass = null;
            try {
                responseClass = (Class<? extends RPCResponse>) Class.forName(responseClassString);
            } catch (ClassNotFoundException | ClassCastException e) {
                e.printStackTrace();
                LoggerEx.error(TAG, "RPC type " + requestType + " don't have correct class name " + responseClassString + ". " + e.getMessage());
                throw new CoreException(ChatErrorCodes.ERROR_RPC_TYPE_REQUEST_NOMAPPING, "RPC type " + requestType + " don't have correct class name " + responseClassString + ". " + e.getMessage());
            }
            if (requestClass != null && responseClass != null) {
                entity = new RPCEntity();
                entity.requestClass = request.getClass();
                entity.responseClass = responseClass;
            }
            RPCEntity previousEntity = typeEntityMap.putIfAbsent(requestType, entity);
            if (previousEntity != null)
                entity = previousEntity;
        }
        return entity;
    }

    private void handleRequest(RPCRequest request) throws CoreException {
        byte[] requestData = request.getData();
        String requestType = request.getType();
        if (requestType == null)
            throw new CoreException(ChatErrorCodes.ERROR_RPC_REQUESTTYPE_ILLEGAL, "RPCRequest type is null");
        if (requestData == null) {
            Byte encode = request.getEncode();
            if (encode == null)
                request.setEncode(RPCRequest.ENCODE_PB);
            try {
                request.persistent();
                if (request.getData() == null)
                    throw new CoreException(ChatErrorCodes.ERROR_RPC_REQUESTDATA_NULL, "RPCRequest data is still null");
            } catch (Throwable t) {
                LoggerEx.error(TAG, "Persistent RPCRequest " + request.getType() + " failed " + t.getMessage() + " server : " + server.toString() + ", serverHost : " + serverHost + " port " + rmiPort);
                throw new CoreException(ChatErrorCodes.ERROR_RPC_PERSISTENT_FAILED, "Persistent RPCRequest " + request.getType() + " failed " + t.getMessage());
            }
        }
    }

    private RPCResponse handleResponse(String requestType, RPCRequest request, byte[] data) throws CoreException, IllegalAccessException, InstantiationException {
        RPCEntity entity = getRPCEntityForClient(requestType, request);
        RPCResponse response = entity.responseClass.newInstance();
        response.setRequest(request);
        response.setData(data);
        response.setEncode(request.getEncode());
        response.setType(request.getType());

        try {
            response.resurrect();
        } catch (Throwable t) {
            LoggerEx.error(TAG, "RPCResponse " + requestType + " resurrect failed, " + t.getMessage());
            throw new CoreException(ChatErrorCodes.ERROR_RPC_RESURRECT_FAILED, "RPCResponse " + requestType + " resurrect failed, " + t.getMessage());
        }
        return response;
    }

    RPCEntity getRPCEntityForServer(String requestType, Class<RPCServerAdapter> serverAdapterClass) throws CoreException {
        RPCEntity entity = typeEntityMap.get(requestType);
        if (entity == null) {
            Class<? extends RPCRequest> requestClass = null;
            Class<? extends RPCResponse> responseClass = null;
            Type[] types = serverAdapterClass.getGenericInterfaces();
            for (Type type : types) {
                if (type instanceof ParameterizedType) {
                    ParameterizedType pType = (ParameterizedType) type;
                    if (pType.getRawType().equals(RPCServerAdapter.class)) {
                        Type[] params = pType.getActualTypeArguments();
                        if (params != null && params.length == 2) {
                            requestClass = (Class<? extends RPCRequest>) params[0];
                            responseClass = (Class<? extends RPCResponse>) params[1];
                        }
                    }
                }
            }

            if (requestClass != null && responseClass != null) {
                entity = new RPCEntity();
                entity.requestClass = requestClass;
                entity.responseClass = responseClass;
            } else {
                throw new CoreException(ChatErrorCodes.ERROR_RPC_ILLEGAL, "RequestClass " + requestClass + " and ResponseClass " + responseClass + " is not prepared for requestType " + requestType);
            }
            RPCEntity previousEntity = typeEntityMap.putIfAbsent(requestType, entity);
            if (previousEntity != null)
                entity = previousEntity;
        }
        return entity;
    }

    public String getRmiId() {
        return rmiId;
    }

    public Integer getRmiPort() {
        return rmiPort;
    }

    public void setRmiPort(Integer rmiPort) {
        this.rmiPort = rmiPort;
    }

    public String getServerHost() {
        return serverHost;
    }

    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }

    public Long getExpireTime() {
        return expireTime;
    }

    @Override
    public boolean isConnected() {
        return status.get() == STATUS_CONNECTED;
    }

    public DisconnectedAfterRetryListener getDisconnectedAfterRetryListener() {
        return disconnectedAfterRetryListener;
    }

    public void setDisconnectedAfterRetryListener(DisconnectedAfterRetryListener disconnectedAfterRetryListener) {
        this.disconnectedAfterRetryListener = disconnectedAfterRetryListener;
    }

    @Override
    public Integer getAverageLatency() {
//        if (averageCounter != null)
//            return averageCounter.getAverage();
        return null;
    }

    public void setRmiId(String rmiId) {
        this.rmiId = rmiId;
    }
}
