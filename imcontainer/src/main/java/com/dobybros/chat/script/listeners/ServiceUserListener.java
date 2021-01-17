package com.dobybros.chat.script.listeners;

import chat.logs.LoggerEx;
import com.dobybros.chat.open.data.IMConfig;
import com.dobybros.chat.open.data.Message;
import com.dobybros.chat.open.data.MsgResult;
import com.dobybros.chat.script.IMRuntimeContext;
import com.dobybros.chat.script.communicator.IMCommunicator;
import org.apache.commons.lang.StringUtils;

import java.util.List;

/**
 * Created by hzj on 2021/1/3 下午3:53
 */
public abstract class ServiceUserListener extends DataServiceUserSessionListener {

    /**
     * Will call this method when user(or room) will connected
     * @return The special config
     */
    public abstract IMConfig getIMConfig();

    /**
     * Will call this method when the server of current user(or room) received the message from any device(or member) in current user(or room),
     * in general, this message will be send to the other users(or rooms)
     * @param message The message that the device(or member) created
     * @param terminal The message sender(the terminal of device(or member))
     * @return message's result, will send to the message sender.
     */
    public abstract MsgResult messageReceived(Message message, Integer terminal);

    /**
     * Will call this method when the server of current user(or room) received the message from any device(or member) in current user(or room),
     * in general, this message will send to the current user(or room)'s other devices(or members)
     * @param message The message that the device(or member) created
     * @param terminal The message sender(the terminal of device(or member))
     * @return message's result, will send to the message sender.
     */
    public abstract MsgResult dataReceived(Message message, Integer terminal);

    /**
     * If should or not send message to client, will call this method before the server of current user(or room) send message to client.
     * @param message the message will be send to client
     * @return if return true, this message will not send to the receivers of current user(or room). default true.
     */
    public abstract Boolean shouldSendMessageToClient(Message message);

    /**
     * Will call this method when the server of current user(or room) received the message from other user(or room).
     * @param message The message other user(or room) sent.
     */
    public abstract void messageReceivedFromOthers(Message message);

    /**
     * Will call this method when the device(or member) pingTime + IMConfig's pingInterval < serverCurrentTime.
     * @param terminal The device(or member)'s terminal the ping timeout.
     */
    public abstract void pingTimeoutReceived(Integer terminal);


    public void sessionCreated() {
        if(backUpMemory()){
            Object data = getRoomDataFromMonitor();//get RoomData from monitor
            if(data != null){
                saveRoomData(data);
            }
            restoreData();
        }
    }

    public List<Integer> channelRegisted(Integer terminal) {
        return null;
    }

    public void channelConnected(Integer terminal) {}

    public void channelClosed(Integer terminal, int reason) {}

    public void sessionClosed(int reason) {
        if(backUpMemory()){
            removeMonitorRoomData(reason);
        }
    }

    @Deprecated
    public Long getMaxInactiveInterval() {
        return null;
    }

}
