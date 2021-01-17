package com.dobybros.chat.open.listeners;

import chat.logs.LoggerEx;
import com.dobybros.chat.open.data.Message;
import com.dobybros.chat.script.IMRuntimeContext;
import com.dobybros.chat.script.communicator.IMCommunicator;
import com.dobybros.chat.script.listeners.ServiceUserListener;
import org.apache.commons.lang.StringUtils;

import java.util.List;

/**
 * Created by hzj on 2021/1/2 下午6:57
 */

public abstract class RoomStatusListener extends ServiceUserListener {

    /**
     * The room's id
     */
    private String roomId;

    /**
     * If the room has split config, this is subRoom's id
     */
    private String subRoomId;

    /**
     * Before any member joined, will create room call this method
     */
    public abstract void roomCreated();

    /**
     * Will call this method when member will joined.
     * @param terminal The member's terminal that will joined.
     * @return The member's terminals that should be kicked.
     */
    public abstract List<Integer> memberWillJoined(Integer terminal);

    /**
     * Will call this method when any member joined.
     * @param terminal The member's terminal
     */
    public abstract void memberJoined(Integer terminal);

    /**
     * Will call this method when any member in the room disconnected.
     * @param terminal The member's terminal
     * @param reason member left reason
     */
    public abstract void memberLeft(Integer terminal, int reason);

    /**
     * Will call this method when the room's everyone disconnected.
     * @param reason disconnected reason
     */
    public abstract void roomClosed(int reason);

    /**
     * 发给除了发送人所在房间以外的其他所有房间，包括自己机器和别的机器
     * @param message Message need send
     * @param toTerminals If set, the message will send to toTerminals only.
     */
    public void sendClusterMessage(Message message, List<Integer> toTerminals) {
        IMCommunicator.getInstance().sendClusterMessage(message, toTerminals);
    }

    /**
     * 发给发送人自己房间
     * @param message Message need send
     * @param excludeTerminal If set, the message will send to terminals except excludeTerminal.
     * @param toTerminal If set, the message will send to toTerminals only. In general, this param and param excludeTerminal not use at the same time.
     */
    public void sendMessageToSelf(Message message, Integer excludeTerminal, Integer toTerminal) {
        IMCommunicator.getInstance().sendMessageToSelf(message, excludeTerminal, toTerminal);
    }

    /**
     * Send message to other.
     * @param message Message need send
     * @param excludeTerminal If set, the message will send to terminals except excludeTerminal.
     * @param toTerminal If set, the message will send to toTerminals only. In general, this param and param excludeTerminal not use at the same time.
     */
    public void sendMessageToOther(Message message, Integer excludeTerminal, Integer toTerminal) {
        IMCommunicator.getInstance().sendMessageToOther(message, excludeTerminal, toTerminal);
    }

    /**
     * Get the instance that add @RoomStatusListener annotation.
     * @param userId the user's id
     * @param service the user's service
     * @return instance
     */
    public ServiceUserListener getServiceUserListener(String userId, String service) {
        return IMCommunicator.getInstance().getServiceUserListener(userId, service);
    }

    /**
     * Close user session.
     * @param userId The user's id
     * @param service The user's service
     */
    public void closeUserSession(String userId, String service) {
        IMCommunicator.getInstance().closeUserSession(userId, service);
    }

    /**
     * Close user channel.
     * @param userId The user's id
     * @param service The user's service
     * @param terminal The terminal of close
     */
    public void closeUserChannel(String userId, String service, Integer terminal) {
        IMCommunicator.getInstance().closeUserChannel(userId, service, terminal);
    }

    @Override
    public void sessionCreated() {
        super.sessionCreated();
        roomCreated();
    }

    @Override
    public List<Integer> channelRegisted(Integer terminal) {
        return memberWillJoined(terminal);
    }

    @Override
    public void channelConnected(Integer terminal) {
        super.channelConnected(terminal);
        memberJoined(terminal);
    }

    @Override
    public void channelClosed(Integer terminal, int reason) {
        super.channelClosed(terminal, reason);
        memberLeft(terminal, reason);
    }

    @Override
    public void sessionClosed(int reason) {
        super.sessionClosed(reason);
        roomClosed(reason);
    }

    @Override
    public void setUserId(String userId) {
        super.setUserId(userId);
        subRoomId = userId;
    }

    @Override
    public void setParentUserId(String parentUserId) {
        super.setParentUserId(parentUserId);
        roomId = parentUserId;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getSubRoomId() {
        return subRoomId;
    }
}