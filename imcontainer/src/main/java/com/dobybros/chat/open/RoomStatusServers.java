package com.dobybros.chat.open;

import com.dobybros.chat.open.data.Message;
import com.dobybros.chat.open.listeners.RoomStatusListener;
import com.dobybros.chat.script.communicator.IMCommunicator;

import java.util.List;

/**
 * Created by hzj on 2021/1/3 下午10:38
 */
public class RoomStatusServers {
    /**
     * Send message to members
     * @param message Message need send
     * @param excludeTerminal If set, the message will send to terminals except excludeTerminal.
     * @param toTerminal If set, the message will send to toTerminals only. In general, this param and param excludeTerminal not use at the same time.
     */
    public void sendMessageToMembers(Message message, Integer excludeTerminal, Integer toTerminal) {
        IMCommunicator.getInstance().sendMessageToSelf(message, excludeTerminal, toTerminal);
    }

    /**
     * Send message to other subRoom
     * @param message Message need send
     * @param toTerminals If set, the message will send to toTerminals only.
     */
    public void sendMessageToSubRoom(Message message, List<Integer> toTerminals) {
        IMCommunicator.getInstance().sendClusterMessage(message, toTerminals);
    }

    /**
     * Send message to other room or user.
     * @param message Message need send
     * @param excludeTerminal If set, the message will send to terminals except excludeTerminal.
     * @param toTerminal If set, the message will send to toTerminals only. In general, this param and param excludeTerminal not use at the same time.
     */
    public void sendMessageToOther(Message message, Integer excludeTerminal, Integer toTerminal) {
        IMCommunicator.getInstance().sendMessageToOther(message, excludeTerminal, toTerminal);
    }

    /**
     * Get the instance that add @RoomStatusListener annotation.
     * @param roomId the room's id
     * @param service the room's service
     * @return instance
     */
    public RoomStatusListener getRoomStatusListener(String roomId, String service) {
        return (RoomStatusListener)IMCommunicator.getInstance().getServiceUserListener(roomId, service);
    }

    /**
     * Close room, RoomStatusListener will receive roomClosed callback.
     * @param roomId The room's id
     * @param service The room's service
     */
    public void closeRoom(String roomId, String service) {
        IMCommunicator.getInstance().closeUserSession(roomId, service);
    }

    /**
     * Close user channel.
     * @param roomId The user's id
     * @param service The user's service
     * @param terminal The member's terminal need kick.
     */
    public void kickMember(String roomId, String service, Integer terminal) {
        IMCommunicator.getInstance().closeUserChannel(roomId, service, terminal);
    }

    /**
     * Close sub rooms except excludeSubRoomId.
     * @param roomId The room's id
     * @param excludeSubRoomId Shouldn't close subRoom
     * @param service The room's service
     */
    public void closeOtherSubRooms(String roomId, String excludeSubRoomId, String service) {
        IMCommunicator.getInstance().closeClusterSessions(roomId, excludeSubRoomId, service);
    }
}
