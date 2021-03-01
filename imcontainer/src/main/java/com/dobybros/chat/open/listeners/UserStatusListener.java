package com.dobybros.chat.open.listeners;

import com.dobybros.chat.open.data.Message;
import com.dobybros.chat.script.communicator.IMCommunicator;
import com.dobybros.chat.script.listeners.ServiceUserListener;

import java.util.List;

/**
 * Created by hzj on 2021/1/2 下午6:57
 */

public abstract class UserStatusListener extends ServiceUserListener {

    /**
     * Will call this method before user's any device connected.
     */
    public abstract void userConnected();

    /**
     * Will call this method when user's any device will connected.
     * @param terminal The device's terminal that will connected.
     * @return The devices that should be kicked.
     */
    public abstract List<Integer> deviceWillConnected(Integer terminal);

    /**
     * Will call this method when user's any device connected.
     * @param terminal The device's terminal
     */
    public abstract void deviceConnected(Integer terminal);

    /**
     * Will call this method when user's any device disconnected.
     * @param terminal The device's terminal
     * @param reason Device disconnected reason
     */
    public abstract void deviceDisconnected(Integer terminal, int reason);

    /**
     * Will call this method when user's every device disconnected.
     * @param reason disconnected reason
     */
    public abstract void userDisconnected(int reason);


    @Override
    public void sessionCreated() {
        super.sessionCreated();
        userConnected();
    }

    @Override
    public List<Integer> channelRegisted(Integer terminal) {
        return deviceWillConnected(terminal);
    }

    @Override
    public void channelConnected(Integer terminal) {
        super.channelConnected(terminal);
        deviceConnected(terminal);
    }

    @Override
    public void channelClosed(Integer terminal, int reason) {
        super.channelClosed(terminal, reason);
        deviceDisconnected(terminal, reason);
    }

    @Override
    public void sessionClosed(int reason) {
        super.sessionClosed(reason);
        userDisconnected(reason);
    }
}
