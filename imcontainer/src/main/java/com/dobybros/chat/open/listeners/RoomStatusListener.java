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