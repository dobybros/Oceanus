package oceanus.sdk.core.net.adapters.data;

import com.alibaba.fastjson.annotation.JSONField;
import oceanus.sdk.core.net.NetworkCommunicator;

import java.util.concurrent.ConcurrentHashMap;

public class ContentPacket<T> extends Packet {
    private static final ConcurrentHashMap<String, Long> serviceKeyCRCCacheMap;
    private static final ConcurrentHashMap<Long, String> crcServiceKeyCacheMap;

    private static final ConcurrentHashMap<Class<?>, Long> classCRCCacheMap;
    private static final ConcurrentHashMap<Long, Class<?>> crcClassCacheMap;
    static {
        serviceKeyCRCCacheMap = new ConcurrentHashMap<>();
        crcServiceKeyCacheMap = new ConcurrentHashMap<>();

        classCRCCacheMap = new ConcurrentHashMap<>();
        crcClassCacheMap = new ConcurrentHashMap<>();
    }
    private long contentType;
    private T content;
    private Long serviceKey;

    @JSONField(serialize = false)
    private boolean committed = false;

    @Override
    public String toString() {
        return "ContentPacket content " + (content != null ? content.toString() : "null");
    }

    public ContentPacket() {
        super(NetworkCommunicator.PACKET_TYPE_CONTENT);
    }
    private ContentPacket(long contentType, T content, Long serviceKey) {
        super(NetworkCommunicator.PACKET_TYPE_CONTENT);
        this.contentType = contentType;
        this.content = content;
        this.serviceKey = serviceKey;
    }

    public static long getServiceKeyCRC(String serviceKey) {
        Long crc = serviceKeyCRCCacheMap.get(serviceKey);
        if(crc == null) {
            java.util.zip.CRC32 x1 = new java.util.zip.CRC32();
            x1.update((serviceKey).getBytes());
            long value = x1.getValue();
            serviceKeyCRCCacheMap.putIfAbsent(serviceKey, value);
            crcServiceKeyCacheMap.putIfAbsent(value, serviceKey);
            return value;
        } else {
            return crc;
        }
    }

    public static String getCRCServiceKey(long crc) {
        return crcServiceKeyCacheMap.get(crc);
    }

    public static long getClassCRC(Class<?> contentClass) {
        Long crc = classCRCCacheMap.get(contentClass);
        if(crc == null) {
            java.util.zip.CRC32 x1 = new java.util.zip.CRC32();
            x1.update((contentClass.getSimpleName()).getBytes());
            long value = x1.getValue();
            classCRCCacheMap.putIfAbsent(contentClass, value);
            crcClassCacheMap.putIfAbsent(value, contentClass);
            return value;
        } else {
            return crc;
        }
    }

    public static Class<?> getCRCClass(long crc) {
        return crcClassCacheMap.get(crc);
    }

    private ContentPacket(Class<T> contentClass) {
        super(NetworkCommunicator.PACKET_TYPE_CONTENT);
        this.contentType = ContentPacket.getClassCRC(contentClass);
    }

    public static <T> ContentPacket<T> buildWithContent(T content, boolean needReliable) {
        ContentPacket<T> contentPacket = ContentPacket.buildWithContent(content);
        contentPacket.setNeedReliable(needReliable);
        return contentPacket;
    }

    public static <T> ContentPacket<T> buildWithContent(T content) {
        return new ContentPacket<>(content);
    }

    static <T> ContentPacket<T> buildWithClass(Class<T> contentClass) {
        return new ContentPacket<>(contentClass);
    }
    public static <T> ContentPacket<T> buildWithTypeAndContentAndServiceKey(long contentType, T content, Long serviceKey) {
        return new ContentPacket<>(contentType, content, serviceKey);
    }

    private ContentPacket(T content) {
        super(NetworkCommunicator.PACKET_TYPE_CONTENT);
        this.content = content;
        this.contentType = ContentPacket.getClassCRC(content.getClass());
    }

    public T getContent() {
        return content;
    }

    public void setContent(T content) {
        this.content = content;
    }

//    public static void main(String... strs) {
//        HashMap<String, Object> map = new HashMap<>();
//        HashMap<Long, Object> map1 = new HashMap<>();
//        Object obj = new Object();
//        String str = "abcdefg";
//        int times = 1000000;
//        map.put(str, obj);
//        java.util.zip.CRC32 x = new java.util.zip.CRC32();
//        x.update(str.getBytes());
//        long key = x.getValue();
//        map1.put(key, obj);
//
//        long time = System.currentTimeMillis();
//        for (int i = 0; i < times; i++) {
//            map.get(str);
//        }
//        System.out.println("map get takes " + (System.currentTimeMillis() - time));
//
//        java.util.zip.CRC32 x1 = new java.util.zip.CRC32();
//        x1.update(str.getBytes());
//        Long k = x.getValue();
//        time = System.currentTimeMillis();
//        Class<?> contentClass = Packet.class;
//        map.put(contentClass.getName(), 23424324L);
//        for (int i = 0; i <times; i++) {
//            map1.get(map.get(contentClass.getName()));
//        }
//        System.out.println("crc get takes " + (System.currentTimeMillis() - time));
//    }


    public long getContentType() {
        return contentType;
    }

    public void setContentType(long contentType) {
        this.contentType = contentType;
    }

    public Long getServiceKey() {
        return serviceKey;
    }

    public void setServiceKey(Long serviceKey) {
        this.serviceKey = serviceKey;
    }

    public void setServiceKey(String serviceKey) {
        this.serviceKey = getServiceKeyCRC(serviceKey);
    }

    public boolean isCommitted() {
        return committed;
    }

    public void setCommitted(boolean committed) {
        this.committed = committed;
    }
}
