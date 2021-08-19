package oceanus.sdk.core.utils;

import com.google.common.primitives.Ints;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ByteUtils {
    public static void copyBytes(byte[] targetBytes, Collection<Integer> sourcesInts) {
        copyBytes(0, targetBytes, sourcesInts);
    }

    public static void copyBytes(int destPos, byte[] targetBytes, Collection<Integer> sourcesInts) {
        int pos = destPos;
        for(Integer value : sourcesInts) {
            byte[] bytes = Ints.toByteArray(value);
            System.arraycopy(bytes, 0, targetBytes, pos, bytes.length);
            pos += bytes.length;
        }
    }

    public static void copyBytes(byte[] targetBytes, byte[]... sourcesBytes) {
        copyBytes(0, targetBytes, sourcesBytes);
    }
    public static void copyBytes(int destPos, byte[] targetBytes, byte[]... sourcesBytes) {
       int pos = destPos;
       for(int i = 0; i < sourcesBytes.length; i++) {
           System.arraycopy(sourcesBytes[i], 0, targetBytes, pos, sourcesBytes[i].length);
           pos += sourcesBytes[i].length;
       }
    }

    public static void copyBytes(int destPos, byte[] targetBytes, List<Integer> values) {
        int pos = destPos;
        for(int i = 0; i < values.size(); i++) {
            byte[] valueBytes = Ints.toByteArray(values.get(i)); //4 bytes
            System.arraycopy(valueBytes, 0, targetBytes, pos, valueBytes.length);
            pos += valueBytes.length;
        }
    }

    public static List<Integer> readIntList(int destPos, byte[] targetBytes, int length) {
        int pos = destPos;
        List<Integer> list = new ArrayList<>();
        for(int i = 0; i < length; i++) {
            int offset = pos + i * 4;
            int value = Ints.fromBytes(targetBytes[offset], targetBytes[offset + 1], targetBytes[offset + 2], targetBytes[offset + 3]);
            list.add(value);
        }
        return list;
    }
}
