package oceanus.sdk.rpc;

import oceanus.apis.CoreException;
import oceanus.sdk.utils.ConcurrentHashSet;

public abstract class RPCClientAdapter {
	protected ConcurrentHashSet<ClientAdapterStatusListener> statusListeners = new ConcurrentHashSet<>();
//	private List<String> remoteServerFutureList = new ArrayList();
	
	public static abstract class ClientAdapterStatusListener {

		public void started(String serverName) {
		}

		public void connected(String serverName) {
		}

		public void disconnected(String serverName) {
		}

		public void terminated(String serverName) {
		}
		
		public void called(String serverName, RPCRequest request,
				RPCResponse response) {
		}

		public void callFailed(String serverName, RPCRequest request,
				CoreException e) throws CoreException {
		}
	}

	public abstract RPCResponse call(RPCRequest request) throws CoreException;

//	public abstract void callAsync(RPCRequest request) throws CoreException;

	public abstract void touch();

	public abstract void clientStart() throws CoreException;

	public abstract void clientDestroy();

	public abstract boolean isConnected();
	
//	public CallListener getCallListener() {
//		return callListener;
//	}

//	public ClientAdapterStatusListener getStatusListener() {
//		return statusListener;
//	}

	public void addStatusListener(ClientAdapterStatusListener statusListener) {
		if(statusListener != null) {
			statusListeners.add(statusListener);
		}
	}
//	public void addToRemoteServerFutureList(String callbackFutureId) {
//		if(callbackFutureId != null && !remoteServerFutureList.contains(callbackFutureId)){
//			remoteServerFutureList.add(callbackFutureId);
//		}
//	}
//	public void clearRemoteServerFutureList(String host){
//		for (String callbackFutureId : remoteServerFutureList){
//			AsyncRpcFuture asyncRpcFuture = RpcCacheManager.getInstance().getAsyncRpcFuture(callbackFutureId);
//			if(asyncRpcFuture != null){
//				CompletableFuture completableFuture = asyncRpcFuture.getFuture();
//				if(completableFuture != null){
//					completableFuture.completeExceptionally(new CoreException(ChatErrorCodes.ERROR_SERVER_CONNECT_FAILED, "Check server alive failed, server host: " + host));
//				}
//			}
//		}
//	}
//	public void removeFromServerFutureList(String callbackFutureId){
//		if(callbackFutureId != null){
//			remoteServerFutureList.remove(callbackFutureId);
//		}
//	}
	public abstract Integer getAverageLatency();
}
