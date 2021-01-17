package com.dobybros.chat.script.communicator;

import chat.config.BaseConfiguration;
import chat.logs.LoggerEx;
import com.dobybros.chat.open.data.IMConfig;
import com.dobybros.chat.open.data.Message;
import com.dobybros.chat.open.data.MsgResult;
import com.dobybros.chat.script.IMRuntimeContext;
import com.dobybros.chat.script.listeners.ServiceUserListener;
import com.docker.utils.BeanFactory;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Properties;

/**
 * Created by hzj on 2021/1/3 下午8:39
 */
public class IMCommunicator {

    public static final String TAG = IMCommunicator.class.getSimpleName();

    private static volatile IMCommunicator instance;
    private CommunicateListener communicateListener;

    public static IMCommunicator getInstance() {
        if (instance == null) {
            synchronized (IMCommunicator.class) {
                if (instance == null) {
                    instance = new IMCommunicator();
                }
            }
        }
        return instance;
    }

    /* ----------------------------------- for service --------------------------------- */

    /**
     * 发给除了发送人所在房间以外的其他所有房间，包括自己机器和别的机器
     * @param message Message need send
     * @param toTerminals If set, the message will send to toTerminals only.
     */
    public void sendClusterMessage(Message message, List<Integer> toTerminals) {
        if (message != null && communicateListener != null)
            try {
                communicateListener.sendClusterMessage(message, toTerminals);
            } catch (Throwable t) {
                LoggerEx.error(TAG, "send cluster message error, message " + message + " toTerminals " + toTerminals + ", eMsg: " + t.getMessage());
            }
    }

    /**
     * 发给发送人自己房间
     * @param message Message need send
     * @param excludeTerminal If set, the message will send to terminals except excludeTerminal.
     * @param toTerminal If set, the message will send to toTerminals only. In general, this param and param excludeTerminal not use at the same time.
     */
    public void sendMessageToSelf(Message message, Integer excludeTerminal, Integer toTerminal) {
        if (message != null && communicateListener != null)
            try {
                communicateListener.sendOutgoingData(message, excludeTerminal, toTerminal);
            } catch (Throwable t) {
                LoggerEx.error(TAG, "send outgoing data error, message " + message + " excludeTerminal " + excludeTerminal + " toTerminals " + toTerminal + ", eMsg: " + t.getMessage());
            }
    }

    /**
     * Send message to other.
     * @param message Message need send
     * @param excludeTerminal If set, the message will send to terminals except excludeTerminal.
     * @param toTerminal If set, the message will send to toTerminals only. In general, this param and param excludeTerminal not use at the same time.
     */
    public void sendMessageToOther(Message message, Integer excludeTerminal, Integer toTerminal) {
        if (message != null && communicateListener != null)
            try {
                communicateListener.sendMessage(message, excludeTerminal, toTerminal);
            } catch (Throwable t) {
                LoggerEx.error(TAG, "send message error, message " + message + " excludeTerminal " + excludeTerminal + " toTerminals " + toTerminal + ", eMsg: " + t.getMessage());
            }
    }

    /**
     * Get the instance that add @RoomStatusListener annotation.
     * @param userId the user's id
     * @param service the user's service
     * @return instance
     */
    public ServiceUserListener getServiceUserListener(String userId, String service) {
        IMRuntimeContext runtimeContext = getRuntimeContext(service);
        if (runtimeContext != null) {
            return runtimeContext.getServiceUserListener(userId, service);
        }
        return null;
    }

    /**
     * Close user session.
     * @param userId The user's id
     * @param service The user's service
     */
    public void closeUserSession(String userId, String service) {
        if (StringUtils.isNotBlank(userId) && StringUtils.isNotBlank(service) && communicateListener != null)
            try {
                communicateListener.closeUserSession(userId, service);
            } catch (Throwable t) {
                LoggerEx.error(TAG, "close user session error, userId " + userId + " service " + service + ", eMsg: " + t.getMessage());
            }
    }

    /**
     * Close user channel.
     * @param userId The user's id
     * @param service The user's service
     * @param terminal The terminal of close
     */
    public void closeUserChannel(String userId, String service, Integer terminal) {
        if (StringUtils.isNotBlank(userId) && StringUtils.isNotBlank(service) && communicateListener != null)
            try {
                communicateListener.closeUserChannel(userId, service, terminal);
            } catch (Throwable t) {
                LoggerEx.error(TAG, "close user channel error, userId " + userId + " service " + service + ", eMsg: " + t.getMessage());
            }
    }

    public void closeClusterSessions(String parentId, String userId, String service) {
        if (StringUtils.isNotBlank(userId) && StringUtils.isNotBlank(service) && communicateListener != null)
            try {
                communicateListener.closeClusterSessions(parentId, userId, service);
            } catch (Throwable t) {
                LoggerEx.error(TAG, "close cluster session channel error, userId " + userId + " service " + service + ", eMsg: " + t.getMessage());
            }
    }

    /* ----------------------------------- start for gateway --------------------------------- */

    public List<Integer> channelRegistered(String userId, String service, Integer terminal) {
        IMRuntimeContext context = getRuntimeContext(service);
        if (context != null)
            return context.channelRegistered(userId, service, terminal);
        return null;
    }

    public void channelCreated(String userId, String service, Integer terminal) {
        IMRuntimeContext context = getRuntimeContext(service);
        if (context != null)
            context.channelCreated(userId, service, terminal);
    }

    public void channelClosed(String userId, String service, Integer terminal, int close) {
        IMRuntimeContext context = getRuntimeContext(service);
        if (context != null)
            context.channelClosed(userId, service, terminal, close);
    }

    public void sessionClosed(String userId, String service, int close) {
        IMRuntimeContext context = getRuntimeContext(service);
        if (context != null)
            context.sessionClosed(userId, service, close);
    }

    public void sessionCreated(String userId, String service) {
        IMRuntimeContext context = getRuntimeContext(service);
        if (context != null)
            context.sessionCreated(userId, service);
    }

    public IMConfig getIMConfig(String userId, String service) {
        IMRuntimeContext context = getRuntimeContext(service);
        if (context != null)
            return context.getIMConfig(userId, service);
        return null;
    }

    public boolean shouldSendMessageToClient(Message message, String userId, String service) {
        IMRuntimeContext context = getRuntimeContext(service);
        if (context != null)
            return context.shouldSendMessageToClient(message, userId, service);
        return true;
    }

    public Long getMaxInactiveInterval(String userId, String service) {
        IMRuntimeContext context = getRuntimeContext(service);
        if (context != null)
            return context.getMaxInactiveInterval(userId, service);
        return null;
    }

    public MsgResult messageReceived(Message message, Integer terminal) {
        IMRuntimeContext context = getRuntimeContext(message.getService());
        if (context != null)
            return context.messageReceived(message, terminal);
        return null;
    }

    public MsgResult dataReceived(Message message, Integer terminal) {
        IMRuntimeContext context = getRuntimeContext(message.getService());
        if (context != null)
            return context.dataReceived(message, terminal);
        return null;
    }

    public void messageReceivedFromUsers(Message message, String receiverId, String receiverService) {
        IMRuntimeContext context = getRuntimeContext(message.getService());
        if (context != null)
            context.messageReceivedFromUsers(message, receiverId, receiverService);
    }

    public void pingTimeoutReceived(String userId, String service, Integer terminal) {
        IMRuntimeContext context = getRuntimeContext(service);
        if (context != null)
            context.pingTimeoutReceived(userId, service, terminal);
    }

    public Properties getServiceConfigProperties(String service) {
        IMRuntimeContext context = getRuntimeContext(service);
        if (context != null && context.getConfiguration() != null)
            return context.getConfiguration().getConfig();
        return null;
    }

    /* ----------------------------------- end for gateway --------------------------------- */

    private IMRuntimeContext getRuntimeContext(String service) {
        if (service != null) {
            BaseConfiguration configuration = (BaseConfiguration) BeanFactory.getBean(BaseConfiguration.class.getName());
            return (IMRuntimeContext)configuration.getRuntimeContext(service);
        }
        return null;
    }

    public CommunicateListener getCommunicateListener() {
        return communicateListener;
    }

    public void setCommunicateListener(CommunicateListener communicateListener) {
        this.communicateListener = communicateListener;
    }

    public interface CommunicateListener {
        public void closeUserSession(String userId, String service);
        public void closeClusterSessions(String parentId, String userId, String service);
        public void closeUserChannel(String userId, String service, Integer terminal);
        public void sendClusterMessage(Message message, List<Integer> toTerminals);
        public void sendOutgoingData(Message message, Integer excludeTerminal, Integer toTerminal);
        public void sendMessage(Message message, Integer excludeTerminal, Integer toTerminal);
    }

}
