package com.docker.rpc;


import chat.config.BaseConfiguration;
import com.docker.oceansbean.BeanFactory;

public abstract class RPCResponse extends RPCBase {
	protected BaseConfiguration baseConfiguration = (BaseConfiguration) BeanFactory.getBean(BaseConfiguration.class.getName());

	protected RPCRequest request;

	public RPCResponse(String type) {
		super(type);
	}

	public RPCRequest getRequest() {
		return request;
	}

	public void setRequest(RPCRequest request) {
		this.request = request;
	}
}
