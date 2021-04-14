package core.net.serializations;

import core.common.AbstractFactory;

public class SerializationStreamFactory extends AbstractFactory<SerializationStreamHandler> {
    public SerializationStreamHandler getSerializationStreamHandler(Class<? extends SerializationStreamHandler> serializationHandlerClass) {
        return get(serializationHandlerClass);
    }
}
