package com.docker.rpc;


import chat.config.BaseConfiguration;
import com.docker.oceansbean.BeanFactory;
import com.docker.rpc.RPCBase;
import com.docker.rpc.RPCRequest;

public abstract class RPCResponse extends RPCBase {
	protected BaseConfiguration baseConfiguration = (BaseConfiguration) BeanFactory.getBean(BaseConfiguration.class.getName());

	protected com.docker.rpc.RPCRequest request;

	public RPCResponse(String type) {
		super(type);
	}

	public com.docker.rpc.RPCRequest getRequest() {
		return request;
	}

	public void setRequest(RPCRequest request) {
		this.request = request;
	}
}
