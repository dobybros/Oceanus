package oceanus.sdk.core.net.rudpex.impl;

import java.util.Collection;
import java.util.concurrent.ConcurrentSkipListMap;

public class MemorySequenceBytesCollector implements SequenceBytesCollector {

    // 排好序的一个整包的小包
    private ConcurrentSkipListMap<Integer, byte[]> sequenceBytesMap = new ConcurrentSkipListMap<>();
    @Override
    public void receivedSequenceBytes(int sequence, byte[] data) {
        sequenceBytesMap.put(sequence, data);
    }

    // 将小包组合成一个大包
    @Override
    public byte[] collectAllBytes() {
//        Collection<Integer> keys = sequenceBytesMap.keySet();
//        int i = 0;
//        for(Integer key : keys) {
//            if(key != i++) {
//                LoggerEx.error(TAG, "");
//            }
//        }
        Collection<byte[]> values = sequenceBytesMap.values();
        int length = 0;
        for (byte[] array : values) {
            length += array.length;
        }
        byte[] result = new byte[length];
        int pos = 0;
        for (byte[] array : values) {
            System.arraycopy(array, 0, result, pos, array.length);
            pos += array.length;
        }
        return result;
    }

    @Override
    public void completed() {

    }

    @Override
    public void clear() {
        sequenceBytesMap.clear();
    }
}
