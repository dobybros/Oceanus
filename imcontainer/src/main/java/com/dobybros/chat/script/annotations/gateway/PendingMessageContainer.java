package com.dobybros.chat.script.annotations.gateway;

import com.dobybros.chat.open.data.Message;
import com.dobybros.gateway.onlineusers.OnlineUser;

import java.util.List;

public class PendingMessageContainer {
    public static final Integer CHANNELCREATED = 1;
    public static final Integer CHANNELNOTCREATED = 0;

    public int type = CHANNELNOTCREATED;
    public List<Message> pendingMessages;
    public List<Message> pendingDatas;
    public OnlineUser onlineUser;
    public Boolean needTcpResult;

    public static String getKey(String userId, String service, Integer terminal) {
        return userId + "#" + service + "@" + terminal;
    }
}