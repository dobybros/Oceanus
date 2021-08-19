package oceanus.services.gatewaymanager.service;

import oceanus.services.gatewaymanager.entity.InstantMessage;
import oceanus.services.gatewaymanager.entity.LoginInfo;

import java.util.List;

public interface GatewayManagerService {
    String SERVICE = "gatewaymanager";

    int TERMINAL_IOS = 10;
    int TERMINAL_ANDROID = 20;

    /**
     * 用户登录Gateway的接口
     *
     * @param userId               用户ID
     * @param gatewayService       指定Gateway服务的名称
     * @param authorisedExpression 授权访问的服务_类名_方法名的正则表达式， 只有匹配的才可以访问， 可以为空， 就使用默认正则表达式， 如果非空， 就会废弃默认正则表达式， 采用该值作为正则表达式
     * @param deviceToken          用于推送的设备唯一ID， 可以是FCM或者是APN的唯一设备ID
     * @param terminal             设备类型， 用于支出deviceToken是那个推送系统， 是APN还是FCM， 或者是别的
     * @param activeLogin          是否是用户主动登录， 如果是主动登录， 会踢掉上一个活跃通道。
     * @param authorisedToken      来自于业务层， 用于安全性问题， 在GameSessionHandler的子类会回调verifyAuthorisedToken进行业务层验证， 此方法并没有强制业务层实现， 按需实现
     * @return
     */
    LoginInfo login(String userId, String gatewayService, String authorisedExpression, String deviceToken, Integer terminal, Boolean activeLogin, String authorisedToken);

    /**
     * 从某个gateway服务退出
     * @param userId
     * @param gatewayService
     */
    void logout(String userId, String gatewayService);

    /**
     * 发送消息给指定用户ID列表
     *
     * @param instantMessage
     * @param toUserIds
     * @return
     */
    List<String> sendInstantMessageReturnNotReceived(InstantMessage instantMessage, List<String> toUserIds);

    /**
     * 发送世界消息， 给所有Gateway服务器
     * {@link InstantMessage#gatewayService} 会指定发给某一类的gateway服务
     *
     * @param instantMessage
     */
    void sendGlobalMessage(InstantMessage instantMessage);

    /**
     * 获得某个用户ID在gateway服务中的相关信息
     * @param userId
     * @param gatewayService
     * @return
     */
    LoginInfo getLoginInfo(String userId, String gatewayService);
}