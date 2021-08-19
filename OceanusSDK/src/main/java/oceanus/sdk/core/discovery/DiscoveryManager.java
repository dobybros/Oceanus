package oceanus.sdk.core.discovery;

import oceanus.sdk.core.common.InternalTools;
import oceanus.sdk.core.net.ContentPacketListener;
import oceanus.sdk.core.net.NetworkCommunicator;
import oceanus.sdk.core.net.data.RequestTransport;
import oceanus.sdk.utils.state.StateListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Aquaman's discovery server side implementation.
 *
 * Aquaman会提供两种网站，
 *  一种是用于某一个Owner所有节点的应用程序管理（上传应用， 删除应用），节点健康监测， 节点服务重启等。
 *  另外一种是Starfish超级管理网站， 用于对Owner进行管理， 管理访问白名单等功能。
 *
 * Aquaman也会是一个节点， Owner是Starfish， 我们可以管理Starfish节点， 用来重新部署Aquaman的网站服务， 这些网站也是通过Groovy.zip部署。
 *
 * DiscoveryManager不提供网站服务， 但是是网站功能的实现者。 例如上传了一个Groovy.zip， DiscoveryManager会负责帮这些数据同步到其他Aquaman节点。
 *
 *
 */
public abstract class DiscoveryManager {
    protected InternalTools internalTools;

    protected int port = 36666;
    /**
     * start Aquaman's discovery at port 16666
     */
    public abstract void start() throws IOException;
    public abstract void start(int discoveryPort) throws IOException;

    public abstract void stop();

    public int getPort() {
        return port;
    }

    public abstract <T extends RequestTransport<?>, P extends ContentPacketListener<T>> DiscoveryManager registerContentPacketClass(Class<T> clazz, Class<P> packetListenerClass);
    /**
     * 注册该服务， 并且广播到其他Aquaman节点
     *
     * @param owner
     * @param project
     * @param service
     * @param version
     * @param serviceStream
     */
    public void registerServiceAndBroadcast(String owner, String project, String service, int version, InputStream serviceStream) {}

    /**
     * 反注册改服务， 并且广播到其他Aquaman节点
     *
     * @param owner
     * @param project
     * @param service
     * @param version
     */
    public void unregisterServiceAndBroadcast(String owner, String project, String service, int version) {}
    public abstract DiscoveryManager addStateListener(StateListener<Integer, NetworkCommunicator> stateListener);
    public abstract boolean removeStateListener(StateListener<Integer, NetworkCommunicator> stateListener);
    public abstract Map<String, Object> memory();

    public abstract DiscoveryInfo getDiscoveryInfo();

}
