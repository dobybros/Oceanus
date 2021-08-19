package oceanus.sdk.core.common;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.Timer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * Created by lick on 2020/10/20.
 * Description：
 */
public class InternalTools{
    private Timer timer;
    private ScheduledExecutorService scheduledExecutorService;

    void setTimer(Timer timer) {
        this.timer = timer;
    }

    public Timer getTimer() {
        return timer;
    }

    public ScheduledExecutorService getScheduledExecutorService() {
        if(scheduledExecutorService == null) {
            synchronized (InternalTools.class) {
                if(scheduledExecutorService == null) {
                    ThreadFactory namedThreadFactory =
                            new ThreadFactoryBuilder().setNameFormat("InternalTools-Scheduled-ThreadPool-%d").build();
                    scheduledExecutorService = Executors.newScheduledThreadPool(CoreRuntime.getCpuCores(), namedThreadFactory);
                }
            }
        }
        return scheduledExecutorService;
    }

}