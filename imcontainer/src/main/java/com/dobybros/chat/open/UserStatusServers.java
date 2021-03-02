package com.dobybros.chat.open;

import com.dobybros.chat.open.data.Message;
import com.dobybros.chat.open.listeners.UserStatusListener;
import com.dobybros.chat.script.communicator.IMCommunicator;

import java.util.List;

/**
 * Created by hzj on 2021/1/3 下午10:38
 */
public class UserStatusServers {
    /**
     * Send message to devices of self
     * @param message Message need send
     * @param excludeTerminal If set, the message will send to devices' terminals except excludeTerminal.
     * @param toTerminal If set, the message will send to toTerminals only. In general, this param and param excludeTerminal not use at the same time.
     */
    public void sendMessageToSelf(Message message, Integer excludeTerminal, Integer toTerminal) {
        IMCommunicator.getInstance().sendMessageToSelf(message, excludeTerminal, toTerminal);
    }

    /**
     * Send message to other users.
     * @param message Message need send
     * @param excludeTerminal If set, the message will send to devices' terminals except excludeTerminal.
     * @param toTerminal If set, the message will send to toTerminals only. In general, this param and param excludeTerminal not use at the same time.
     */
    public void sendMessageToOtherUsers(Message message, Integer excludeTerminal, Integer toTerminal) {
        IMCommunicator.getInstance().sendMessageToOther(message, excludeTerminal, toTerminal);
    }

    /**
     * Get the instance that add @UserStatusListener annotation.
     * @param userId the user's id
     * @param service the user's service
     * @return instance
     */
    public UserStatusListener getUserStatusListener(String userId, String service) {
        return (UserStatusListener)IMCommunicator.getInstance().getServiceUserListener(userId, service);
    }

    /**
     * Disconnected user, UserStatusListener will receive userDisconnected callback
     * @param userId The user's id
     * @param service The user's service
     */
    public void disconnectedUser(String userId, String service) {
        IMCommunicator.getInstance().closeUserSession(userId, service);
    }

    /**
     * Disconnected device.
     * @param userId The user's id
     * @param service The user's service
     * @param terminal The device's terminal should disconnected.
     */
    public void disconnectedDevice(String userId, String service, Integer terminal) {
        IMCommunicator.getInstance().closeUserChannel(userId, service, terminal);
    }

}
