package oceanus.sdk.core.net.rudpex.impl;

/**
     * Could collect bytes in to memory or file.
     */
    public interface SequenceBytesCollector {
        void receivedSequenceBytes(int sequence, byte[] data);

        /**
         * If collect bytes into file, this method will throw error. Only work when collect into memory.
         * 
         * @return
         */
        byte[] collectAllBytes();

        void completed();

        void clear();
    }