package oceanus.sdk.core.net.serializations;


import oceanus.sdk.core.common.AbstractFactory;

public class SerializationStreamFactory extends AbstractFactory<SerializationStreamHandler> {
    public SerializationStreamHandler getSerializationStreamHandler(Class<? extends SerializationStreamHandler> serializationHandlerClass) {
        return get(serializationHandlerClass);
    }
}
