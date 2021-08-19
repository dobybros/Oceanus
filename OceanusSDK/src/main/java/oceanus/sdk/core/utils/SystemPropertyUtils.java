package oceanus.sdk.core.utils;

public class SystemPropertyUtils {
    public static int readInt(String key, int defaultValue) {
        String valueStr = System.getProperty(key);
        int value = defaultValue;
        if(valueStr != null) {
            try {
                value = Integer.parseInt(valueStr);
            } catch (Throwable t) {
            }
        }
        return value;
    }

    public static long readLong(String key, long defaultValue) {
        String valueStr = System.getProperty(key);
        long value = defaultValue;
        if(valueStr != null) {
            try {
                value = Integer.parseInt(valueStr);
            } catch (Throwable t) {
            }
        }
        return value;
    }

    public static String readString(String key, String defaultValue) {
        String valueStr = System.getProperty(key);
        String value = defaultValue;
        if(valueStr != null) {
            value = valueStr;
        }
        return value;
    }
}
