package com.dobybros.gateway.channels.msgs;

import chat.errors.CoreException;
import com.dobybros.chat.annotation.MessageReceived;
import com.dobybros.chat.binary.data.Data;
import com.dobybros.gateway.onlineusers.OnlineUserManager;
import com.dobybros.gateway.onlineusers.impl.OnlineUserManagerImpl;
import org.apache.mina.core.session.IoSession;
import com.docker.oceansbean.BeanFactory;

public abstract class MessageReceivedListener {
	private Class<? extends Data> dataClass;
	
	protected OnlineUserManager onlineUserManager = (OnlineUserManager) BeanFactory.getBean(OnlineUserManagerImpl.class.getName());

	public abstract void messageReceived(Data data, IoSession session)
			throws CoreException;

	public Class<? extends Data> getDataClass() {
		if(dataClass == null) {
			MessageReceived messageReceived = this.getClass().getAnnotation(MessageReceived.class);
			if(messageReceived != null) 
				dataClass = messageReceived.dataClass();
		}
		return dataClass;
	}

}