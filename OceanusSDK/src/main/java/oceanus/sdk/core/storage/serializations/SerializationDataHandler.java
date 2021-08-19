package oceanus.sdk.core.storage.serializations;

public interface SerializationDataHandler {
    /**
     * Object to byte array
     *
     * @param object
     * @param <T>
     * @return
     */
    <T> byte[] convert(T object);

    /**
     * byte array to Object by specified class
     *
     * @param data
     * @param clazz
     * @param <T>
     * @return
     */
    <T> T convert(byte[] data, Class<T> clazz);

}
