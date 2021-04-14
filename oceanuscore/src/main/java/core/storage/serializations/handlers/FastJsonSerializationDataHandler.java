package core.storage.serializations.handlers;

import com.alibaba.fastjson.JSON;
import core.log.LoggerHelper;
import core.storage.serializations.SerializationDataHandler;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class FastJsonSerializationDataHandler implements SerializationDataHandler {
    @Override
    public <T> byte[] convert(T object) {
        if(object == null) {
            LoggerHelper.logger.error("Convert object is null");
            return null;
        }
        if(object instanceof byte[]) {
            return (byte[]) object;
        }
        if(object instanceof String) {
            try {
                return ((String) object).getBytes("utf8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                LoggerHelper.logger.error("Unsupport utf8 encode, error " + e.getMessage() + " use default for encoding bytes to object.");
                return ((String) object).getBytes();
            }
        }
        String json = JSON.toJSONString(object);
        try {
            return json.getBytes("utf8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            LoggerHelper.logger.error("Unsupport utf8 encode, error " + e.getMessage() + " use default for encoding bytes to string.");
            return json.getBytes();
        }
    }

    @Override
    public <T> T convert(byte[] data, Class<T> clazz) {
        if(data == null || clazz == null) {
            LoggerHelper.logger.error("Illegal parameter for converting data to object");
            return null;
        }

        if(clazz.equals(byte[].class)) {
            return (T) data;
        }
        if(clazz.equals(String.class)) {
            try {
                return (T) new String(data, "utf8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                LoggerHelper.logger.error("Unsupport utf8 decode, error " + e.getMessage() + " use default for decoding bytes to object.");
                return (T) new String(data);
            }
        }
        try {
            T t = JSON.parseObject(new String(data, "utf8"), clazz);
            return t;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            LoggerHelper.logger.error("Unsupport utf8 decode, error " + e.getMessage() + " use default for decoding bytes to object.");
            T t = JSON.parseObject(new String(data), clazz);
            return t;
        } catch (Throwable t) {
            t.printStackTrace();
            LoggerHelper.logger.error("Parse data for class " + clazz + " failed, " + t.getMessage() + " return null for this data, size " + data.length + (data.length < 1024 ? new String(data) : new String(Arrays.copyOf(data, 1024))));
            return null;
        } finally {
        }
    }

}
