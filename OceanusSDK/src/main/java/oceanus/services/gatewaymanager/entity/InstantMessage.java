package oceanus.services.gatewaymanager.entity;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;

public class InstantMessage<T extends InstantContent> {

    public static final String FIELD_USER_ID = "uId";
    public static final String FIELD_EXPIRE_TIME = "eTime";
    public static final String FIELD_GROUPID = "gId";
    public static final String FIELD_TIME = "time";
    public static final String FIELD_CONTENT_TYPE = "cType";
    public static final String FIELD_CONTENT_STR = "cStr";
    public static final String FIELD_GATEWAY_SERVICE = "gateSer";


    private Boolean pushWhenOffline;
    /**
     * 如果expireTime为空， 就不会做离线存储。
     * 否则该消息会被优先存储， 以及过了expireTime后被删除。
     */
    private Long expireTime;
    /**
     * 当 #cacheTimeKey不为空时，表示在网关 GatewaySessionHandler#cacheKeyToTimeMap缓存该玩家，收到该类事件的时间。
     * 当玩家登录后，要主动通知玩家上次该类事件的时间，客户端对比后，如果发现不同，则主动获取；否则使用客户端缓存，节省资源；
     */
    @JSONField(name = "ctKey")
    String cacheTimeKey;

    /**
     * 来自于哪个用户ID
     */
    private String userId;
    /**
     * 来自于哪个聊天组ID
     */
    private String groupId;
    private String id;
    private Long time;

    /**
     * 消息类型
     */
    private String contentType;
    /**
     * 消息体
     */
    private String contentStr;
    /**
     * 网关类型，发往哪种类型的网关goldcore.constant.GatewayServiceConstant
     */
    private String gatewayService;

    @JSONField(serialize = false, deserialize = false)
    private T content;

    public InstantMessage toContentStr(boolean forceUpdate) {
        if (content != null) {
            if (contentStr == null || forceUpdate)
                contentStr = JSON.toJSONString(content);
        }
        return this;
    }

    public InstantMessage toContentStr() {
        return toContentStr(false);
    }

    public void fromContent(Class<T> tClass) {
        if (contentStr != null)
            content = JSON.parseObject(contentStr, tClass);
    }

    public String getCacheTimeKey() {
        return cacheTimeKey;
    }

    public void setCacheTimeKey(String cacheTimeKey) {
        this.cacheTimeKey = cacheTimeKey;
    }

    public Boolean getPushWhenOffline() {
        return pushWhenOffline;
    }

    public void setPushWhenOffline(Boolean pushWhenOffline) {
        this.pushWhenOffline = pushWhenOffline;
    }

    public Long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Long expireTime) {
        this.expireTime = expireTime;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentStr() {
        return contentStr;
    }

    public void setContentStr(String contentStr) {
        this.contentStr = contentStr;
    }

    public String getGatewayService() {
        return gatewayService;
    }

    public void setGatewayService(String gatewayService) {
        this.gatewayService = gatewayService;
    }

    public T getContent() {
        return content;
    }

    public void setContent(T content, boolean toContentStr) {
        this.content = content;
        if (toContentStr)
            this.toContentStr();
    }

    public void setContent(T content) {
        setContent(content, false);
    }
}
