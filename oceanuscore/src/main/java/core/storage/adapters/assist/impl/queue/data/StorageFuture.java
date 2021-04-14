package core.storage.adapters.assist.impl.queue.data;

import core.common.InternalTools;
import core.utils.thread.ThreadPoolFactory;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.*;

/**
 * Created by lick on 2020/11/16.
 * Description：
 */
public class StorageFuture<T> extends CompletableFuture {
    public String key;
    public T value;
    public Long expire;
    public Integer option;
    public Long length = 1L;

    protected StorageFuture(String key, T value, Integer option, Long expire){
        this.key = key;
        this.value = value;
        this.option = option;
        this.expire = expire;
    }
}
