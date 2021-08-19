package oceanus.sdk.core.net.serializations;


import oceanus.sdk.core.storage.serializations.SerializationDataHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface SerializationStreamHandler extends SerializationDataHandler {
    /**
     * Object to output stream, possible to network output stream or other stream.
     *
     * @param object
     * @param os
     * @param <T>
     */
    <T> void convert(T object, OutputStream os) throws IOException;

    /**
     * covert to object from input stream, possible from network input stream.
     * so the input stream doesn't mean all data is the object, handler need to read out certain bytes to generate object by specified class.
     *
     * @param is
     * @param clazz
     * @param <T>
     * @return
     */
    <T> T convert(InputStream is, Class<T> clazz);

    /**
     * give up unknown data in input stream.
     *
     * @param is
     */
    void consume(InputStream is);
}
