package oceanus.sdk.core.discovery;

import oceanus.sdk.core.common.CoreRuntime;
import oceanus.sdk.core.discovery.impl.client.NodeRegistrationHandlerImpl;
import oceanus.sdk.core.discovery.impl.server.DiscoveryManagerImpl;
import oceanus.sdk.core.utils.SystemPropertyUtils;
import oceanus.sdk.logger.LoggerEx;

public final class DiscoveryRuntime extends CoreRuntime {
    private static final String TAG = DiscoveryRuntime.class.getSimpleName();
    private static NodeRegistrationHandler nodeRegistrationHandler;
    private static DiscoveryManager discoveryManager;

    public static NodeRegistrationHandler getNodeRegistrationHandler() {
        return nodeRegistrationHandler;
    }
    public static NodeRegistrationHandler getAndInitNodeRegistrationHandler(int publicUdpPort) {
        if(nodeRegistrationHandler == null) {
            synchronized (DiscoveryRuntime.class) {
                if(nodeRegistrationHandler == null) {
                    Class<? extends NodeRegistrationHandler> nodeRegistrationHandlerClass = null;
                    String nodeRegistrationHandlerClassStr = SystemPropertyUtils.readString("starfish.discovery.registration.class", null);
                    if(nodeRegistrationHandlerClassStr != null) {
                        try {
                            nodeRegistrationHandlerClass = (Class<? extends NodeRegistrationHandler>) Class.forName(nodeRegistrationHandlerClassStr);
                            nodeRegistrationHandler = nodeRegistrationHandlerClass.getConstructor().newInstance();
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                            LoggerEx.error(TAG, "Class not found while read from system property \"starfish.discovery.registration.class\", " + nodeRegistrationHandlerClassStr);
                        } catch (Throwable t) {
                            t.printStackTrace();
                            LoggerEx.error(TAG, "Unknown error occurred while read from system property \"starfish.discovery.registration.class\", " + nodeRegistrationHandlerClassStr + " error " + t.getMessage());
                        }
                    }
                    if(nodeRegistrationHandler == null) {
                        nodeRegistrationHandlerClass = NodeRegistrationHandlerImpl.class;
                        try {
                            nodeRegistrationHandler = nodeRegistrationHandlerClass.getConstructor().newInstance();
                        } catch (Throwable t) {
                            t.printStackTrace();
                            LoggerEx.error(TAG, "Unknown error occurred while create NodeRegistrationHandler with NodeRegistrationHandlerImpl, error " + t.getMessage());
                        }
                    }
                    nodeRegistrationHandler.init(publicUdpPort);
                    nodeRegistrationHandler.internalTools = CoreRuntime.getInternalTools();
                    LoggerEx.info(TAG, "NodeRegistrationHandler created with starfish.discovery.registration.class " + nodeRegistrationHandlerClass);
                }
            }
        }
        return nodeRegistrationHandler;
    }

    public static DiscoveryManager getDiscoveryManager() {
        if(discoveryManager == null) {
            synchronized (DiscoveryRuntime.class) {
                if(discoveryManager == null) {
                    Class<? extends DiscoveryManager> discoveryManagerClass = null;
                    String discoveryManagerClassStr = SystemPropertyUtils.readString("starfish.discovery.class", null);
                    if(discoveryManagerClassStr != null) {
                        try {
                            discoveryManagerClass = (Class<? extends DiscoveryManager>) Class.forName(discoveryManagerClassStr);
                            discoveryManager = discoveryManagerClass.getConstructor().newInstance();
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                            LoggerEx.error(TAG, "Class not found while read from system property \"starfish.discovery.class\", " + discoveryManagerClassStr);
                        } catch (Throwable t) {
                            t.printStackTrace();
                            LoggerEx.error(TAG, "Unknown error occurred while read from system property \"starfish.discovery.class\", " + discoveryManagerClassStr + " error " + t.getMessage());
                        }
                    }
                    if(discoveryManager == null) {
                        discoveryManagerClass = DiscoveryManagerImpl.class;
                        try {
                            discoveryManager = discoveryManagerClass.getConstructor().newInstance();
                        } catch (Throwable t) {
                            t.printStackTrace();
                            LoggerEx.error(TAG, "Unknown error occurred while create DiscoveryManager with DiscoveryManagerImpl, error " + t.getMessage());
                        }
                    }
                    discoveryManager.internalTools = CoreRuntime.getInternalTools();
                    LoggerEx.info(TAG, "DiscoveryManager created with starfish.discovery.class " + discoveryManagerClass);
                }
            }
        }
        return discoveryManager;
    }
}
