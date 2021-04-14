package core.storage.serializations;

import core.common.AbstractFactory;

public class SerializationDataFactory extends AbstractFactory<SerializationDataHandler> {
    public SerializationDataHandler getSerializationDataHandler(Class<? extends SerializationDataHandler> serializationDataClass) {
        return get(serializationDataClass);
    }
}
