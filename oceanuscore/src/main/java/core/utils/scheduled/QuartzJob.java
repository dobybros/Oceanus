package core.utils.scheduled;

import core.utils.thread.MultipleFixedThreadManager;
import core.utils.thread.ThreadFactory;
import core.utils.thread.ThreadPoolFactory;
import org.apache.commons.lang.StringUtils;
import org.quartz.*;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by lick on 2019/10/10.
 * Description：
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class QuartzJob implements Job {
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        ScheduleTask task = (ScheduleTask) jobExecutionContext.getMergedJobDataMap().get("ScheduleTask");
        if(task != null){
            MultipleFixedThreadManager.getInstance().execute(ThreadPoolFactory.getInstance().getTimerThreadPool(), 1, task, task.getId(), null);
        }
    }
}
