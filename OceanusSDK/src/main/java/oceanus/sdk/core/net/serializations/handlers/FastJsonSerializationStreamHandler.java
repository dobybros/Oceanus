package oceanus.sdk.core.net.serializations.handlers;

import com.alibaba.fastjson.JSON;
import oceanus.sdk.core.net.serializations.SerializationStreamHandler;
import oceanus.sdk.logger.LoggerEx;

import java.io.*;
import java.util.Arrays;

public class FastJsonSerializationStreamHandler implements SerializationStreamHandler {
    private static final String TAG = FastJsonSerializationStreamHandler.class.getSimpleName();

    @Override
    public <T> byte[] convert(T object) {
        if(object == null) {
            LoggerEx.error(TAG, "Convert object is null");
            return null;
        }
        String json = JSON.toJSONString(object);
        try {
            return getBytes(json.getBytes("utf8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            LoggerEx.error(TAG, "Unsupport utf8 encode, error " + e.getMessage() + " use default for decoding bytes to string.");
            return getBytes(json.getBytes());
        }
    }

    private byte[] getBytes(byte[] data) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(outputStream);
        try{
            dos.writeInt(data.length);
            dos.write(data);
            dos.flush();
            return outputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            LoggerEx.error(TAG, "getBytes " + data.length + " failed, " + e.getMessage());
        } finally {
            try {
                outputStream.close();
            } catch (Throwable ignored) { }
            try {
                dos.close();
            } catch (Throwable ignored) { }
        }
        return null;
    }

    @Override
    public <T> void convert(T object, OutputStream os) throws IOException {
        byte[] data = convert(object);
        os.write(data);
    }

    @Override
    public <T> T convert(byte[] data, Class<T> clazz) {
        if(data == null || clazz == null) {
            LoggerEx.error(TAG, "Illegal parameter for converting data to object");
            return null;
        }
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
        byte[] theData = getDataBytes(inputStream);
        if(theData == null)
            return null;

        try {
            T t = JSON.parseObject(new String(theData, "utf8"), clazz);
            return t;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            LoggerEx.error(TAG, "Unsupport utf8 decode, error " + e.getMessage() + " use default for decoding bytes to object.");
            T t = JSON.parseObject(new String(theData), clazz);
            return t;
        } catch (Throwable t) {
            t.printStackTrace();
            LoggerEx.error(TAG, "Parse data for class " + clazz + " failed, " + t.getMessage() + " return null for this data, size " + theData.length + (theData.length < 1024 ? new String(theData) : new String(Arrays.copyOf(theData, 1024))));
            return null;
        } finally {
            try {
                inputStream.close();
            } catch (Throwable ignored) { }
        }
    }

    private byte[] getDataBytes(InputStream is) {
        DataInputStream dis = new DataInputStream(is);
        try {
            int length = dis.readInt();
            byte[] theData = new byte[length];
            dis.readFully(theData);
            return theData;
        } catch (IOException e) {
            e.printStackTrace();
            LoggerEx.error(TAG, "getDataBytes failed, " + e.getMessage());
        }
        return null;
    }

    @Override
    public <T> T convert(InputStream is, Class<T> clazz) {
        byte[] theData = getDataBytes(is);
        if(theData == null)
            return null;

        try {
            T t = JSON.parseObject(new String(theData, "utf8"), clazz);
            return t;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            LoggerEx.error(TAG, "Unsupport utf8 decode, error " + e.getMessage() + " use default for decoding bytes to object.");
            T t = JSON.parseObject(new String(theData), clazz);
            return t;
        } catch (Throwable t) {
            t.printStackTrace();
            LoggerEx.error(TAG, "Parse data for class " + clazz + " failed, " + t.getMessage() + " return null for this data, size " + theData.length + (theData.length < 1024 ? new String(theData) : new String(Arrays.copyOf(theData, 1024))));
            try {
                LoggerEx.error(TAG, "errorData " + new String(theData, "utf8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    @Override
    public void consume(InputStream is) {
        getDataBytes(is);
    }
}
