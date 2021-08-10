package com.docker.rpc.impl;

import chat.errors.CoreException;
import chat.logs.LoggerEx;
import com.docker.errors.CoreErrorCodes;
import com.docker.rpc.*;
import com.docker.rpc.annotations.RPCServerHandler;
import oceanus.sdk.rpc.impl.RMIServer;
import org.apache.commons.lang.StringUtils;
import script.core.runtime.AbstractRuntimeContext;
import script.core.runtime.groovy.object.GroovyObjectEx;
import script.core.runtime.handler.annotation.clazz.ClassAnnotationGlobalHandler;

import java.lang.annotation.Annotation;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RMIServerImplWrapper extends ClassAnnotationGlobalHandler {
	RMIServerHandler rmiServerHandler;
	//Server
	Map<String, GroovyObjectEx<RPCServerAdapter>> serverAdapterMap;

	Integer port;

	RMIServer server;

	RPCServerMethodInvocation serverMethodInvocation;

	public RMIServer initServer() {
		if(server == null) {
			try {
				server = new RMIServerImpl(port + 1, this);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		return server;
	}

	@Override
	public void handleAnnotatedClassesInjectBean(AbstractRuntimeContext runtimeContext) {
		if(serverAdapterMap != null){
			for (GroovyObjectEx<RPCServerAdapter> groovyObjectEx : serverAdapterMap.values()) {
				try {
					groovyObjectEx = (GroovyObjectEx<RPCServerAdapter>) getObject(null, groovyObjectEx.getGroovyClass(), runtimeContext);
				}catch (CoreException e){
					LoggerEx.error(TAG, e.getMessage());
				}
			}
		}
	}

	public void initClient(RMIServer server) {
		this.server = server;
	}

	public RMIServer getServer() {
		return server;
	}

	public RMIServerImplWrapper(Integer port) throws RemoteException {
//		super(port + 1);
		this.port = port;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -4853473944368414096L;
	private static final String TAG = RMIServerImplWrapper.class.getSimpleName();

	public RPCResponse onCall(RPCRequest request) throws CoreException {
		String type = request.getType();
		if(type == null)
			throw new CoreException(CoreErrorCodes.ERROR_RPC_TYPE_NOMAPPING, "No server adapter found by type " + type);

		if(request instanceof MethodRequest) {
			if(serverMethodInvocation != null)
				serverMethodInvocation = new RPCServerMethodInvocation();
			RPCResponse response = serverMethodInvocation.onCall((MethodRequest) request);
			return response;
		}

		//LickLulu, please remove below for me.
		GroovyObjectEx<RPCServerAdapter> adapter = serverAdapterMap.get(type);
		if(adapter == null)
			throw new CoreException(CoreErrorCodes.ERROR_RPC_TYPE_NOSERVERADAPTER, "No server adapter found by type " + type);



		RPCServerAdapter serverAdapter = null;
		serverAdapter = adapter.getObject();
		RPCResponse response1 = serverAdapter.onCall(request);
		return response1;
	}

	@Override
	public Class<? extends Annotation> handleAnnotationClass() {
		return RPCServerHandler.class;
	}

	@Override
	public void handleAnnotatedClasses(Map<String, Class<?>> annotatedClassMap, AbstractRuntimeContext runtimeContext) throws CoreException {
		if (annotatedClassMap != null && !annotatedClassMap.isEmpty()) {
			StringBuilder uriLogs = new StringBuilder(
					"\r\n---------------------------------------\r\n");

			Map<String, GroovyObjectEx<RPCServerAdapter>> newServerAdapterMap = new ConcurrentHashMap<>();
			Set<String> keys = annotatedClassMap.keySet();
			for (String key : keys) {
				Class<?> groovyClass = annotatedClassMap.get(key);

				// Class<GroovyServlet> groovyClass =
				// groovyServlet.getGroovyClass();
				if (groovyClass != null) {
					// Handle RequestIntercepting
					RPCServerHandler requestIntercepting = groovyClass.getAnnotation(RPCServerHandler.class);
					if (requestIntercepting != null) {
						String rpcType = processAnnotationString(runtimeContext, requestIntercepting.rpcType());
						if (!StringUtils.isBlank(rpcType)) {
							GroovyObjectEx<RPCServerAdapter> serverAdapter = (GroovyObjectEx<RPCServerAdapter>) getObject(null, groovyClass, runtimeContext);
							if (serverAdapter != null) {
								uriLogs.append("RPCServerHandler " + rpcType + "#" + groovyClass + "\r\n");
								newServerAdapterMap.put(rpcType, serverAdapter);
							}
						}
					}
				}
			}
			this.serverAdapterMap = newServerAdapterMap;
			uriLogs.append("---------------------------------------");
			LoggerEx.info(TAG, uriLogs.toString());
		}
	}

	@Override
	public Object getKey() {
		return RMIServerImplWrapper.class;
	}

	public RMIServerHandler getRmiServerHandler() {
		return rmiServerHandler;
	}

	public void setRmiServerHandler(RMIServerHandler rmiServerHandler) {
		this.rmiServerHandler = rmiServerHandler;
	}

	public Map<String, GroovyObjectEx<RPCServerAdapter>> getServerAdapterMap() {
		return serverAdapterMap;
	}

	public RPCServerMethodInvocation getServerMethodInvocation() {
		return serverMethodInvocation;
	}

	public void setServerAdapterMap(Map<String, GroovyObjectEx<RPCServerAdapter>> serverAdapterMap) {
		this.serverAdapterMap = serverAdapterMap;
	}

	public void setServerMethodInvocation(RPCServerMethodInvocation serverMethodInvocation) {
		this.serverMethodInvocation = serverMethodInvocation;
	}

}

