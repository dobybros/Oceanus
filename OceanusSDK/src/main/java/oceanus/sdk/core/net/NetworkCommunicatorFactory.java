package oceanus.sdk.core.net;

import oceanus.sdk.core.common.AbstractFactory;
import oceanus.sdk.core.common.InternalTools;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

public class NetworkCommunicatorFactory extends AbstractFactory<NetworkCommunicator> {
    private Class<? extends NetworkCommunicator> networkCommunicatorClass;
//    private ConcurrentHashMap<String, Class<?>> contentTypeClassMap = new ConcurrentHashMap<>();

    private String serverName;
    private Long serverNameCRC;
    private String serverPrefix;
    InternalTools internalTools;

    private boolean isNetworkCommunicatorCreated = false;

    public NetworkCommunicatorFactory(Class<? extends NetworkCommunicator> networkCommunicatorClass) {
        this(null, networkCommunicatorClass);
    }

    public NetworkCommunicatorFactory(String serverPrefix, Class<? extends NetworkCommunicator> networkCommunicatorClass) {
        this.serverPrefix = serverPrefix;
        if(StringUtils.isBlank(this.serverPrefix))
            this.serverPrefix = "node";
        this.networkCommunicatorClass = networkCommunicatorClass;
        serverName = this.serverPrefix + "-" + UUID.randomUUID().toString().replace("-", "");//RandomStringUtils.randomAlphanumeric(10);
        java.util.zip.CRC32 x = new java.util.zip.CRC32();
        x.update(serverName.getBytes());
        serverNameCRC = x.getValue();
    }

    public NetworkCommunicator getNetworkCommunicator() {
       return create(networkCommunicatorClass);
    }

    public interface NetworkCommunicatorCreatedListener {
        void created(NetworkCommunicator networkCommunicator);
    }

    public interface  NetworkCommunicatorDestroyedListener {
        void destroyed(NetworkCommunicator networkCommunicator);
    }

    public NetworkCommunicator buildNetworkCommunicator() {
        return createNetworkCommunicator();
    }

//    public NetworkCommunicator startAsServer() throws IOException {
//        return startAsServer(-1);
//    }
//
//    public NetworkCommunicator startAsServer(int port) throws IOException {
//        NetworkCommunicator networkCommunicator = createNetworkCommunicator();
//
//        LoggerEx.info(TAG, "startAsServer " + serverName);
////        networkCommunicator.startServer(port);
//        if(port > 0) {
//            networkCommunicator.startAtFixedPort(port);
//        } else {
//            networkCommunicator.startAtAnyPort();
//        }
//        return networkCommunicator;
//    }

//    private void putContentType(String type, Class<?> contentClass) {
//        if(isNetworkCommunicatorCreated) {
//            LoggerEx.warn(TAG, "[NetworkCommunicatorFactory] Register content type should before create any of NetworkCommunicator, otherwise the register type " + type + " for contentClass " + contentClass + " may not be available.");
//        }
//        contentTypeClassMap.put(type, contentClass);
//    }
//
//
//
//    public void registerContentType(Class<?> contentClass) {
////        contentTypeClassMap.put(contentClass.getSimpleName(), contentClass);
//        putContentType(contentClass.getSimpleName(), contentClass);
//    }
//
//    public void registerContentType(String contentType, Class<?> contentClass) {
////        contentTypeClassMap.put(contentType, contentClass);
//        putContentType(contentType, contentClass);
//    }
//
//    public Class<?> unregisterContentType(Class<?> contentClass) {
//        return contentTypeClassMap.remove(contentClass.getSimpleName());
//    }
//
//    public Class<?> unregisterContentType(String contentType) {
//        return contentTypeClassMap.remove(contentType);
//    }

    private NetworkCommunicator createNetworkCommunicator() {
        if(!isNetworkCommunicatorCreated) {
            isNetworkCommunicatorCreated = true;
        }
        NetworkCommunicator networkCommunicator = create(networkCommunicatorClass);
        networkCommunicator.internalTools = internalTools;
        if(networkCommunicator == null)
            throw new NullPointerException("Start as server failed... networkCommunicatorClass is " + networkCommunicatorClass);
        networkCommunicator.init();
//        networkCommunicator.cloneContentTypeClassMap(contentTypeClassMap);
        networkCommunicator.serverName = serverName;
        networkCommunicator.serverNameCRC = serverNameCRC;
        return networkCommunicator;
    }

    public String getServerName() {
        return serverName;
    }

    public Long getServerNameCRC() {
        return serverNameCRC;
    }
}

