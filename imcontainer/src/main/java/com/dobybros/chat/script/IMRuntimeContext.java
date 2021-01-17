package com.dobybros.chat.script;

import chat.config.Configuration;
import chat.errors.CoreException;
import chat.logs.LoggerEx;
import chat.main.ServerStart;
import com.dobybros.chat.open.data.IMConfig;
import com.dobybros.chat.open.data.Message;
import com.dobybros.chat.open.data.MsgResult;
import com.dobybros.chat.script.handlers.annotation.ServiceUserAnnotationHandler;
import com.dobybros.chat.open.listeners.SessionListener;
import com.dobybros.chat.script.handlers.annotation.RoomStatusAnnotationHandler;
import com.dobybros.chat.script.handlers.annotation.UserStatusAnnotationHandler;
import com.dobybros.chat.script.listeners.ServiceUserListener;
import com.docker.script.BaseRuntimeContext;
import org.apache.commons.lang.exception.ExceptionUtils;
import script.core.runtime.groovy.object.GroovyObjectEx;

import java.util.List;

/**
 * Created by lick on 2020/12/23.
 * Description：
 */
public class IMRuntimeContext extends BaseRuntimeContext {
    private final String TAG = IMRuntimeContext.class.getSimpleName();
    public IMRuntimeContext(Configuration configuration) throws CoreException {
        super(configuration);
    }
    private GroovyObjectEx<SessionListener> sessionListener;

    public void setSessionListener(GroovyObjectEx<SessionListener> sessionListener) {
        this.sessionListener = sessionListener;
    }

    /**
     * 这个接口会被调用两次， 一次是http的authorized的时候， 另一次是从tcp通道收到identity的时候
     *
     * @param userId
     * @param terminal
     * @return 返回会被踢下线的其他通道。
     */
    public List<Integer> channelRegistered(String userId, String service, Integer terminal) {
        if (sessionListener != null) {
            try {
                return sessionListener.getObject().channelRegisterd(userId, service, terminal);
            } catch (Throwable t) {
                t.printStackTrace();
                LoggerEx.error(TAG, "Handle channel " + terminal + " regitered by " + userId + " failed, " + ExceptionUtils.getFullStackTrace(t));
            }
        } else {
            ServiceUserListener listener = getServiceUserListener(userId, service);
            if (listener != null) {
                try {
                    return listener.channelRegisted(terminal);
                } catch (Throwable t) {
                    t.printStackTrace();
                    LoggerEx.error(TAG, "Handle channel " + terminal + " regitered by " + userId + "@" + service + " failed, " + ExceptionUtils.getFullStackTrace(t));
                }
            }
        }
        return null;
    }

    public ServiceUserListener getServiceUserListener(String userId, String service) {
        RoomStatusAnnotationHandler handler = (RoomStatusAnnotationHandler) this.getClassAnnotationHandler(RoomStatusAnnotationHandler.class);
        if (handler != null) {
            ServiceUserListener listener = handler.getAnnotatedListener(userId, service);
            if (listener != null)
                return listener;
        }
        UserStatusAnnotationHandler userHandler = (UserStatusAnnotationHandler) this.getClassAnnotationHandler(UserStatusAnnotationHandler.class);
        if (handler != null) {
            ServiceUserListener listener = userHandler.getAnnotatedListener(userId, service);
            if (listener != null)
                return listener;
        }
        return null;
    }

    public void channelCreated(String userId, String service, Integer terminal) {
        if (sessionListener != null) {
            try {
                sessionListener.getObject().channelCreated(userId, service, terminal);
            } catch (Throwable t) {
                t.printStackTrace();
                LoggerEx.error(TAG, "Handle channel " + terminal + " created by " + userId + " failed, " + ExceptionUtils.getFullStackTrace(t));
            }
        } else {
            ServiceUserListener listener = getServiceUserListener(userId, service);
            if (listener != null) {
                try {
                    listener.channelConnected(terminal);
                } catch (Throwable t) {
                    t.printStackTrace();
                    LoggerEx.error(TAG, "Handle channel " + terminal + " created by " + userId + "@" + service + " failed, " + ExceptionUtils.getFullStackTrace(t));
                }
            }
        }
    }

    public void channelClosed(String userId, String service, Integer terminal, int close) {
        if (sessionListener != null) {
            try {
                sessionListener.getObject().channelClosed(userId, service, terminal, close);
            } catch (Throwable t) {
                t.printStackTrace();
                LoggerEx.error(TAG, "Handle channel " + terminal + " closed by " + userId + " failed, " + ExceptionUtils.getFullStackTrace(t));
            }
        } else {
            ServiceUserListener listener = getServiceUserListener(userId, service);
            if (listener != null) {
                try {
                    listener.channelClosed(terminal, close);
                } catch (Throwable t) {
                    t.printStackTrace();
                    LoggerEx.error(TAG, "Handle channel " + terminal + " closed by " + userId + "@" + service + " failed, " + ExceptionUtils.getFullStackTrace(t));
                }
            }
        }
    }

    public void sessionClosed(String userId, String service, int close) {
        if (sessionListener != null) {
            try {
                sessionListener.getObject().sessionClosed(userId, service, close);
            } catch (Throwable t) {
                t.printStackTrace();
                LoggerEx.error(TAG, "Handle session closed by " + userId + " failed, " + ExceptionUtils.getFullStackTrace(t));
            }
        } else {
            ServiceUserListener listener = getServiceUserListener(userId, service);
            if (listener != null) {
                try {
                    listener.sessionClosed(close);
                } catch (Throwable t) {
                    t.printStackTrace();
                    LoggerEx.error(TAG, "Handle session closed by " + userId + "@" + service + " failed, " + ExceptionUtils.getFullStackTrace(t));
                }
            }
        }

        // 删除serviceUserListener
        ServiceUserAnnotationHandler handler = (ServiceUserAnnotationHandler) this.getClassAnnotationHandler(ServiceUserAnnotationHandler.class);
        if (handler != null) {
            try {
                handler.removeListeners(userId, service);
            } catch (Throwable t) {
                t.printStackTrace();
                LoggerEx.error(TAG, "Remove serviceUserListener " + userId + "@" + service + " close failed, " + ExceptionUtils.getFullStackTrace(t));
            }
        }
    }

    public void sessionCreated(String userId, String service) {
        if (sessionListener != null) {
            try {
                sessionListener.getObject().sessionCreated(userId, service);
            } catch (Throwable t) {
                t.printStackTrace();
                LoggerEx.error(TAG, "Handle session created by " + userId + " failed, " + ExceptionUtils.getFullStackTrace(t));
            }
        } else {
            ServiceUserListener listener = getServiceUserListener(userId, service);
            if (listener != null) {
                try {
                    listener.sessionCreated();
                } catch (Throwable t) {
                    t.printStackTrace();
                    LoggerEx.error(TAG, "Handle session created by " + userId + "@" + service + " failed, " + ExceptionUtils.getFullStackTrace(t));
                }
            }
        }
    }

    public IMConfig getIMConfig(String userId, String service) {
        IMConfig imConfig = null;
        if (sessionListener != null) {
            try {
                imConfig = sessionListener.getObject().getIMConfig(userId, service);
            } catch (Throwable t) {
                t.printStackTrace();
                LoggerEx.error(TAG, "Handle session " + userId + " service " + service + " getIMConfig failed, " + ExceptionUtils.getFullStackTrace(t));
            }
        } else {
            ServiceUserListener listener = getServiceUserListener(userId, service);
            if (listener != null) {
                try {
                    imConfig = listener.getIMConfig();
                } catch (Throwable t) {
                    t.printStackTrace();
                    LoggerEx.error(TAG, "Handle session " + userId + " service " + service + " getIMConfig failed, " + ExceptionUtils.getFullStackTrace(t));
                }
            }
        }
        if (imConfig == null) {
            imConfig = new IMConfig();
        }
        return imConfig;
    }

    public boolean shouldSendMessageToClient(Message message, String userId, String service) {
        if (sessionListener != null) {
            try {
                return sessionListener.getObject().shouldInterceptMessageReceivedFromUsers(message, userId, service);
            } catch (Throwable t) {
                t.printStackTrace();
                LoggerEx.error(TAG, "Handle session " + userId + " service " + service + " getIMConfig failed, " + ExceptionUtils.getFullStackTrace(t));
            }
        } else {
            ServiceUserListener listener = getServiceUserListener(userId, service);
            if (listener != null)
                try {
                    return listener.shouldSendMessageToClient(message);
                } catch (Throwable t) {
                    t.printStackTrace();
                    LoggerEx.error(TAG, "Handle session " + userId + " service " + service + " getIMConfig failed, " + ExceptionUtils.getFullStackTrace(t));
                }
        }
        return false;
    }

    public Long getMaxInactiveInterval(String userId, String service) {
        if (sessionListener != null) {
            try {
                return sessionListener.getObject().getMaxInactiveInterval(userId, service);
            } catch (Throwable t) {
                t.printStackTrace();
                LoggerEx.error(TAG, "Handle session " + userId + " service " + service + " getMaxInactiveInterval failed, " + ExceptionUtils.getFullStackTrace(t));
            }
        } else {
            ServiceUserListener listener = getServiceUserListener(userId, service);
            if (listener != null)
                try {
                    listener.getMaxInactiveInterval();
                } catch (Throwable t) {
                    t.printStackTrace();
                    LoggerEx.error(TAG, "Handle session " + userId + " service " + service + " getMaxInactiveInterval failed, " + ExceptionUtils.getFullStackTrace(t));
                }
        }
        return null;
    }

    /**
     * 可以修改Message里的内容
     *
     * @param message
     * @return 非空就不用发送消息了， 为空时会继续发送。 默认为为空
     */
    public MsgResult messageReceived(Message message, Integer terminal) {
        if (sessionListener != null) {
            try {
                return sessionListener.getObject().messageReceived(message, terminal);
            } catch (Throwable t) {
                t.printStackTrace();
                LoggerEx.error(TAG, "Handle received message " + message + " terminal " + terminal + " failed, " + ExceptionUtils.getFullStackTrace(t));
            }
        } else {
            ServiceUserListener listener = getServiceUserListener(message.getUserId(), message.getService());
            if (listener != null) {
                try {
                    return listener.messageReceived(message, terminal);
                } catch (Throwable t) {
                    t.printStackTrace();
                    LoggerEx.error(TAG, "Handle received message " + message + " terminal " + terminal + " failed, " + ExceptionUtils.getFullStackTrace(t));
                }
            }
        }
        return null;
    }

    /**
     * 可以修改Message里的内容
     *
     * @param message
     * @return 非空就不用发送消息了， 为空时会继续发送。 默认为为空
     */
    public MsgResult dataReceived(Message message, Integer terminal) {
        if (sessionListener != null) {
            try {
                return sessionListener.getObject().dataReceived(message, terminal);
            } catch (Throwable t) {
                t.printStackTrace();
                LoggerEx.error(TAG, "Handle received data " + message + " terminal " + terminal + " failed, " + ExceptionUtils.getFullStackTrace(t));
            }
        } else {
            ServiceUserListener listener = getServiceUserListener(message.getUserId(), message.getService());
            if (listener != null) {
                try {
                    return listener.dataReceived(message, terminal);
                } catch (Throwable t) {
                    t.printStackTrace();
                    LoggerEx.error(TAG, "Handle received data " + message + " terminal " + terminal + " failed, " + ExceptionUtils.getFullStackTrace(t));
                }
            }
        }
        return null;
    }

    public void messageReceivedFromUsers(Message message, String receiverId, String receiverService) {
        ServerStart.getInstance().getGatewayThreadPoolExecutor().execute(() -> {
            if (sessionListener != null) {
                try {
                    sessionListener.getObject().messageReceivedFromUsers(message, receiverId, receiverService);
                } catch (Throwable t) {
                    t.printStackTrace();
                    LoggerEx.error(TAG, "Handle message " + message + " messageReceivedFromUsers failed, " + ExceptionUtils.getFullStackTrace(t));
                }
            } else {
                ServiceUserListener listener = getServiceUserListener(receiverId, receiverService);
                if (listener != null) {
                    try {
                        listener.messageReceivedFromOthers(message);
                    } catch (Throwable t) {
                        t.printStackTrace();
                        LoggerEx.error(TAG, "Handle message " + message + " messageReceivedFromUsers failed, " + ExceptionUtils.getFullStackTrace(t));
                    }
                }
            }
        });
    }

    public void pingTimeoutReceived(String userId, String service, Integer terminal) {
        ServerStart.getInstance().getGatewayThreadPoolExecutor().execute(() -> {
            if (sessionListener != null) {
                try {
                    sessionListener.getObject().pingTimeoutReceived(userId, service, terminal);
                } catch (Throwable t) {
                    t.printStackTrace();
                    LoggerEx.error(TAG, "Handle pingReceived failed, " + ExceptionUtils.getFullStackTrace(t));
                }
            } else {
                ServiceUserListener listener = getServiceUserListener(userId, service);
                if (listener != null) {
                    try {
                        listener.pingTimeoutReceived(terminal);
                    } catch (Throwable t) {
                        t.printStackTrace();
                        LoggerEx.error(TAG, "Handle pingReceived failed, " + ExceptionUtils.getFullStackTrace(t));
                    }
                }
            }

        });
    }

}
