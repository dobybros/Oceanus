package com.proxy.im;

import com.dobybros.chat.binary.data.Data;

/**
 * @author lick
 * @date 2019/11/13
 */
public interface SessionContext {
    void setAttribute(Object key, Object value);

    Object getAttribute(Object key);

    Short getEncodeVersion();

    void removeAttribute(Object key);

    void write(Data data);

    void write(byte[] data, byte type);

    void write(int code, String description, String forId);

    void close();

    void close(boolean immediately);

    boolean isClosing();
}
