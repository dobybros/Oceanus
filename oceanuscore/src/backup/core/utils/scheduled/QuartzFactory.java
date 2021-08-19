package core.utils.scheduled;

import core.log.LoggerHelper;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.util.Date;

/**
 * Created by lick on 20220/10/20.
 * Description：
 */
public class QuartzFactory {
    private static final String TAG = QuartzFactory.class.getSimpleName();
    private SchedulerFactory schedulerFactory;
    private static volatile QuartzFactory instance;

    void addJob(ScheduleTask task) throws SchedulerException {
        this.removeJob(task.getId());
        Scheduler sched = schedulerFactory.getScheduler();
        // 任务名，任务组，任务执行类
        JobDetail jobDetail = JobBuilder.newJob(QuartzJob.class).withIdentity(task.getId(), task.getId()).build();
        JobDataMap jobDataMap = jobDetail.getJobDataMap();
        jobDataMap.put("ScheduleTask", task);
        Trigger trigger = null;
        if (task.getPeriod() != null) {
            if (task.getDelay() == null) {
                trigger = TriggerBuilder.newTrigger()
                        .withIdentity(task.getId(), task.getId())
                        .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                .repeatForever()
                                .withIntervalInMilliseconds(task.getPeriod()))
                        .build();
            } else {
                trigger = TriggerBuilder.newTrigger()
                        .withIdentity(task.getId(), task.getId())
                        .startAt(new Date(System.currentTimeMillis() + task.getDelay()))
                        .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                .repeatForever()
                                .withIntervalInMilliseconds(task.getPeriod()))
                        .build();
            }
        } else {
            if (task.getDelay() == null) {
                trigger = TriggerBuilder.newTrigger()
                        .withIdentity(task.getId(), task.getId())
                        .build();
            } else {
                trigger = TriggerBuilder.newTrigger()
                        .withIdentity(task.getId(), task.getId())
                        .startAt(new Date(System.currentTimeMillis() + task.getDelay()))
                        .build();
            }
        }

        if (trigger != null) {
            // 调度容器设置JobDetail和Trigger
            sched.scheduleJob(jobDetail, trigger);

            // 启动
            if (!sched.isShutdown()) {
                sched.start();
            }
        }
    }

    void addJobByScheduletime(ScheduleTask task) throws SchedulerException {
            this.removeJob(task.getId());
            Scheduler sched = schedulerFactory.getScheduler();
            // 任务名，任务组，任务执行类
            JobDetail jobDetail = JobBuilder.newJob(QuartzJob.class).withIdentity(task.getId(), task.getId()).build();
            JobDataMap jobDataMap = jobDetail.getJobDataMap();
            jobDataMap.put("TimerTaskEx", task);
            Trigger trigger = null;
            if ((task.getScheduleTime() < System.currentTimeMillis()) && (task.getScheduleTime() + 5000 > System.currentTimeMillis())) {
                //作业的触发器
                if (task.getPeriod() != null) {
                    trigger = TriggerBuilder.newTrigger()
                            .withIdentity(task.getId(), task.getId())
                            .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                    .repeatForever()
                                    .withIntervalInMilliseconds(task.getPeriod()))
                            .build();
                } else {
                    trigger = TriggerBuilder.newTrigger()
                            .withIdentity(task.getId(), task.getId())
                            .build();
                }
            } else {
                //作业的触发器
                if (task.getPeriod() != null) {
                    trigger = TriggerBuilder.newTrigger()
                            .withIdentity(task.getId(), task.getId())
                            .startAt(new Date(task.getScheduleTime()))
                            .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                    .repeatForever()
                                    .withIntervalInMilliseconds(task.getPeriod()))
                            .build();
                } else {
                    trigger = TriggerBuilder.newTrigger()
                            .withIdentity(task.getId(), task.getId())
                            .startAt(new Date(task.getScheduleTime()))
                            .build();
                }
            }
            if (trigger != null) {
                // 调度容器设置JobDetail和Trigger
                sched.scheduleJob(jobDetail, trigger);

                // 启动
                if (!sched.isShutdown()) {
                    sched.start();
                    LoggerHelper.getLogger().info(QuartzFactory.class.getSimpleName(), "The period task " + task.getId() + " add successful");
                }
            }
    }

    void addCronJob(ScheduleTask task) throws SchedulerException {
        this.removeJob(task.getId());
        Scheduler sched = schedulerFactory.getScheduler();
        // 任务名，任务组，任务执行类
        JobDetail jobDetail = JobBuilder.newJob(QuartzJob.class).withIdentity(task.getId(), task.getId()).build();
        JobDataMap jobDataMap = jobDetail.getJobDataMap();
        jobDataMap.put("TimerTaskEx", task);
        //作业的触发器
        CronTrigger trigger = TriggerBuilder.//和之前的 SimpleTrigger 类似，现在的 CronTrigger 也是一个接口，通过 Tribuilder 的 build()方法来实例化
                newTrigger().
                withIdentity(task.getId(), task.getId()).
                withSchedule(CronScheduleBuilder.cronSchedule(task.getCron())). //在任务调度器中，使用任务调度器的 CronScheduleBuilder 来生成一个具体的 CronTrigger 对象
                build();

        // 调度容器设置JobDetail和Trigger
        sched.scheduleJob(jobDetail, trigger);

        // 启动
        if (!sched.isShutdown()) {
            sched.start();
        }
    }

    void removeJob(String id) throws SchedulerException {
        Scheduler sched = schedulerFactory.getScheduler();

        TriggerKey triggerKey = TriggerKey.triggerKey(id, id);

        sched.pauseTrigger(triggerKey);// 停止触发器
        sched.unscheduleJob(triggerKey);// 移除触发器
        sched.deleteJob(JobKey.jobKey(id, id));// 删除任务

    }

    public void shutdownJobs() throws SchedulerException {
        Scheduler sched = schedulerFactory.getScheduler();
        if (!sched.isShutdown()) {
            sched.shutdown();
        }
    }

    private void setSchedulerFactory(SchedulerFactory schedulerFactory) {
        this.schedulerFactory = schedulerFactory;
    }

    public SchedulerFactory getSchedulerFactory() {
        return schedulerFactory;
    }

    public static QuartzFactory getInstance() {
        if (instance == null) {
            synchronized (QuartzFactory.class){
                if(instance == null){
                    instance = new QuartzFactory();
                    instance.setSchedulerFactory(new StdSchedulerFactory());
                }
            }
        }
        return instance;
    }
}
