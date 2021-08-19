package oceanus.sdk.core.utils.thread;

import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * Created by lick on 2020/11/17.
 * Description：
 */
public class ThreadPoolFactory {
    private ThreadPoolExecutor timerThreadPoolExecutor;
    private ThreadPoolExecutor storageThreadPoolExecutor;
    private static volatile ThreadPoolFactory instance;
    public static ThreadPoolFactory getInstance() {
        if (instance == null) {
            synchronized (ThreadPoolFactory.class) {
                if (instance == null) {
                    instance = new ThreadPoolFactory();
                }
            }
        }
        return instance;
    }
    public ThreadPoolExecutor getTimerThreadPool(){
        if(timerThreadPoolExecutor == null){
            synchronized (ThreadPoolFactory.class){
                if(timerThreadPoolExecutor == null){
                    String coreSizeStr = System.getProperty("timer.thread.core.size");
                    int coreSize;
                    if(StringUtils.isBlank(coreSizeStr)){
                        coreSize = 30;
                    }else {
                        coreSize = Integer.parseInt(coreSizeStr);
                    }
                    String maximumPoolSizeStr = System.getProperty("timer.thread.maxinum.size");
                    int maximumPoolSize;
                    if(StringUtils.isBlank(coreSizeStr)){
                        maximumPoolSize = 150;
                    }else {
                        maximumPoolSize = Integer.parseInt(maximumPoolSizeStr);
                    }
                    String keepAliveStr = System.getProperty("timer.thread.keepAlive");
                    int keepAlive;
                    if(StringUtils.isBlank(coreSizeStr)){
                        keepAlive = 50;
                    }else {
                        keepAlive = Integer.parseInt(keepAliveStr);
                    }
                    String queueSizeStr = System.getProperty("timer.thread.queue.size");
                    int queueSize;
                    if(StringUtils.isBlank(queueSizeStr)){
                        queueSize = 20000;
                    }else {
                        queueSize = Integer.parseInt(queueSizeStr);
                    }
                    timerThreadPoolExecutor = new ThreadPoolExecutor(coreSize, maximumPoolSize, keepAlive, TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>(queueSize), new ThreadFactory("Timer"));
                }
            }
        }
        return timerThreadPoolExecutor;
    }
    public ThreadPoolExecutor getStorageThreadPool(){
        if(storageThreadPoolExecutor == null){
            synchronized (ThreadPoolFactory.class){
                if(storageThreadPoolExecutor == null){
                    String coreSizeStr = System.getProperty("storage.thread.core.size");
                    int coreSize;
                    if(StringUtils.isBlank(coreSizeStr)){
                        coreSize = 30;
                    }else {
                        coreSize = Integer.parseInt(coreSizeStr);
                    }
                    String maximumPoolSizeStr = System.getProperty("storage.thread.maxinum.size");
                    int maximumPoolSize;
                    if(StringUtils.isBlank(coreSizeStr)){
                        maximumPoolSize = 300;
                    }else {
                        maximumPoolSize = Integer.parseInt(maximumPoolSizeStr);
                    }
                    String keepAliveStr = System.getProperty("storage.thread.keepAlive");
                    int keepAlive;
                    if(StringUtils.isBlank(coreSizeStr)){
                        keepAlive = 50;
                    }else {
                        keepAlive = Integer.parseInt(keepAliveStr);
                    }
                    String queueSizeStr = System.getProperty("storage.thread.queue.size");
                    int queueSize;
                    if(StringUtils.isBlank(queueSizeStr)){
                        queueSize = 20000;
                    }else {
                        queueSize = Integer.parseInt(queueSizeStr);
                    }
                    storageThreadPoolExecutor = new ThreadPoolExecutor(coreSize, maximumPoolSize, keepAlive, TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>(queueSize), new ThreadFactory("Timer"));
                }
            }
        }
        return storageThreadPoolExecutor;
    }
    public LongAdder longAdder = new LongAdder();
    public Long time;
}
