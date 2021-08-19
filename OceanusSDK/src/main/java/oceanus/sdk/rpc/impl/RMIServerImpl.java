package oceanus.sdk.rpc.impl;


import oceanus.sdk.core.discovery.errors.CoreErrorCodes;
import oceanus.sdk.errors.ChatErrorCodes;
import oceanus.apis.CoreException;
import oceanus.sdk.logger.LoggerEx;
import oceanus.sdk.rpc.*;
import oceanus.sdk.rpc.remote.stub.ServiceStubManager;
import oceanus.sdk.server.OnlineServer;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RMIServerImpl extends UnicastRemoteObject implements RMIServer {
    RPCServerMethodInvocation serverMethodInvocation;

    public RMIServerImpl(Integer port) throws RemoteException {
        super(port);
        serverMethodInvocation = new RPCServerMethodInvocation();
    }


    /**
     *
     */
    private static final long serialVersionUID = -4853473944368414096L;
    private static final String TAG = RMIServerImpl.class.getSimpleName();

    @Override
    public byte[] call(byte[] data, String type, Byte encode)
            throws RemoteException {
//        if (serverWrapper.rmiServerHandler == null)
//            throw new RemoteException("RPC handler is null");
        RPCResponse response = null;
        try {
            RPCRequest request = null;
            RPCServerAdapter serverAdapter = null;
            RPCEntity entity = null;
            if (MethodRequest.RPCTYPE.equals(type)) {
//                if (serverWrapper.getServerMethodInvocation() == null)
//                    serverWrapper.setServerMethodInvocation(new RPCServerMethodInvocation());
                request = new MethodRequest();

                request.setEncode(encode);
                request.setType(type);
                request.setData(data);
                request.resurrect();
                response = serverMethodInvocation.onCall((MethodRequest) request);
            } else {
                throw new CoreException(CoreErrorCodes.ERROR_RPC_ILLEGAL, "Only support MethodRequest now, but type is " + type);
            }
            if (response != null) {
                byte[] responseData = response.getData();
                if (responseData == null) {
                    if (response.getEncode() == null)
                        response.setEncode(RPCBase.ENCODE_PB);
                    response.persistent();
                }
                return response.getData();
            }
            return null;
        } catch (Throwable t) {
            LoggerEx.error(TAG, "RPC call type " + type + " occur error on server side, " + t.getMessage());
            String message = null;
            if (t instanceof CoreException) {
                message = ((CoreException) t).getCode() + "|" + t.getMessage();
            } else {
                message = t.getMessage();
            }
            throw new RemoteException(message);
        }
    }

    @Override
    public boolean alive() throws RemoteException {
        return true;
    }

    private RPCClientAdapter getClientAdapter(RPCRequest request) throws CoreException {
        if (((MethodRequest) request).getSourceIp() != null && ((MethodRequest) request).getSourcePort() != null && ((MethodRequest) request).getFromServerName() != null) {
            ServiceStubManager serviceStubManager = OnlineServer.getInstance().getServiceStubManagerFactory().get();
            if (serviceStubManager != null) {
                RPCClientAdapterMap clientAdapterMap = null;
//                if (serviceStubManager.getUsePublicDomain()) {
//                    clientAdapterMap = RPCClientAdapterMapFactory.getInstance().getRpcClientAdapterMapSsl();
//                } else {
//                }
                clientAdapterMap = RPCClientAdapterMapFactory.getInstance().getRpcClientAdapterMap();
                RPCClientAdapter clientAdapter = clientAdapterMap.registerServer(((MethodRequest) request).getSourceIp(), ((MethodRequest) request).getSourcePort(), ((MethodRequest) request).getFromServerName());
                if (clientAdapter != null) {
                    return clientAdapter;
                }
            }
        } else {
            LoggerEx.warn(TAG, "The request cant callback async, sourceIp: " + ((MethodRequest) request).getSourceIp() + ", sourcePort: " + ((MethodRequest) request).getSourcePort() + ",fromServerName: " + ((MethodRequest) request).getFromServerName());
        }
        return null;
    }

}

