package core.net;

import core.common.CoreRuntime;
import core.log.LoggerHelper;
import core.net.rudpex.communicator.RUDPEXNetworkCommunicator;
import core.net.serializations.SerializationStreamFactory;
import core.net.serializations.SerializationStreamHandler;
import core.net.serializations.handlers.FastJsonSerializationStreamHandler;

public final class NetRuntime extends CoreRuntime {
    private static SerializationStreamFactory serializationStreamFactory = new SerializationStreamFactory();
    private static Class<? extends SerializationStreamHandler> serializationStreamHandlerClass;

    private static NetworkCommunicatorFactory networkCommunicatorFactory;
    private static Class<? extends NetworkCommunicator> networkCommunicatorClass;

    public static SerializationStreamHandler getSerializationStreamHandler() {
        if(serializationStreamHandlerClass == null) {
            synchronized (NetRuntime.class) {
                if(serializationStreamHandlerClass == null) {
                    String serializationHandlerClassStr = System.getProperty("starfish.serialization.stream.class");
                    if(serializationHandlerClassStr != null) {
                        try {
                            Class<? extends SerializationStreamHandler> clazz = (Class<? extends SerializationStreamHandler>) Class.forName(serializationHandlerClassStr);
                            serializationStreamHandlerClass = clazz;
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                            LoggerHelper.logger.error("Class not found while read from system property \"starfish.serialization.stream.class\", " + serializationHandlerClassStr);
                        } catch (Throwable t) {
                            t.printStackTrace();
                            LoggerHelper.logger.error("Unknown error occurred while read from system property \"starfish.serialization.stream.class\", " + serializationHandlerClassStr + " error " + t.getMessage());
                        }
                    }
                    if(serializationStreamHandlerClass == null)
                        serializationStreamHandlerClass = FastJsonSerializationStreamHandler.class;
                    LoggerHelper.logger.info("starfish.serialization.stream.class is " + serializationStreamHandlerClass);
                }
            }
        }
        return serializationStreamFactory.getSerializationStreamHandler(serializationStreamHandlerClass);
    }

    private static NetworkCommunicatorFactory getNetworkCommunicatorFactory() {
        if(networkCommunicatorFactory == null) {
            synchronized (NetRuntime.class) {
                if(networkCommunicatorFactory == null) {
                    String networkCommunicatorClassStr = System.getProperty("starfish.network.class");
                    if(networkCommunicatorClassStr != null) {
                        try {
                            Class<? extends NetworkCommunicator> clazz = (Class<? extends NetworkCommunicator>) Class.forName(networkCommunicatorClassStr);
                            networkCommunicatorClass = clazz;
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                            LoggerHelper.logger.error("Class not found while read from system property \"starfish.network.class\", " + networkCommunicatorClassStr);
                        } catch (Throwable t) {
                            t.printStackTrace();
                            LoggerHelper.logger.error("Unknown error occurred while read from system property \"starfish.network.class\", " + networkCommunicatorClassStr + " error " + t.getMessage());
                        }
                    }
                    if(networkCommunicatorClass == null)
                        networkCommunicatorClass = RUDPEXNetworkCommunicator.class;
                    networkCommunicatorFactory = new NetworkCommunicatorFactory(networkCommunicatorClass);
                    networkCommunicatorFactory.internalTools = CoreRuntime.getInternalTools();
                    LoggerHelper.logger.info("NetworkCommunicatorFactory created with starfish.network.class " + networkCommunicatorClass);
                }
            }
        }
        return networkCommunicatorFactory;
    }

    public static NetworkCommunicator buildNetworkCommunicator() {
        return getNetworkCommunicatorFactory().buildNetworkCommunicator();
    }

    public static String getServerName() {
        return getNetworkCommunicatorFactory().getServerName();
    }

    public static Long getServerNameCRC() {
        return getNetworkCommunicatorFactory().getServerNameCRC();
    }
}
