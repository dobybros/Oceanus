package core.utils.scheduled;



import chat.logs.LoggerEx;

import java.util.UUID;

public class Timer {
    private static final String TAG = Timer.class.getSimpleName();

    public void schedule(TimerTask timerTask) {
    }

    public interface TimerTask {
        void run();
    }

    public ScheduleTask schedule(TimerTask task, Long delay, Long period) {
        ScheduleTask scheduleTask = new ScheduleTask() {
            @Override
            public void execute() {
                task.run();
            }
        };
        schedule(scheduleTask, delay, period);
        return scheduleTask;
    }

    public ScheduleTask schedule(TimerTask task, String cron) {
        ScheduleTask scheduleTask = new ScheduleTask() {
            @Override
            public void execute() {
                task.run();
            }
        };
        schedule(scheduleTask, cron);
        return scheduleTask;
    }

    public ScheduleTask schedule(TimerTask task, Long delay) {
        ScheduleTask scheduleTask = new ScheduleTask() {
            @Override
            public void execute() {
                task.run();
            }
        };
        schedule(scheduleTask, delay);
        return scheduleTask;
    }

    public void cancel(ScheduleTask task) {
        try {
            if (task.getId() != null) {
                QuartzFactory.getInstance().removeJob(task.getId());
            }
        } catch (Throwable e) {
            e.printStackTrace();
            LoggerEx.error(TAG, "Remove timetask filed, taskId: " + task.getId() + ",e: " + e);
        }
    }


    public void schedule(ScheduleTask task, Long delay) {
        try {
            if (delay != null) {
                task.setDelay(delay);
            }
            if (task.getId() == null) {
                task.setId(task.getDescription() + UUID.randomUUID().toString());
            }
            QuartzFactory.getInstance().addJob(task);
        } catch (Throwable e) {
            LoggerEx.error(TAG, "Schedule scheduleTask " + task + " failed, " + e);
        }
    }

    public void schedule(ScheduleTask task, Long delay, Long period) {
        try {
            if (delay != null) {
                task.setDelay(delay);
            }
            if (period != null) {
                task.setPeriod(period);
            }
            if (task.getId() == null) {
                task.setId(task.getDescription() + UUID.randomUUID().toString());
            }
            QuartzFactory.getInstance().addJob(task);
        } catch (Throwable e) {
            e.printStackTrace();
            LoggerEx.error(TAG, "Schedule Period scheduleTask " + task + " failed, " + e);
        }
    }

    public void schedule(ScheduleTask task, String cron) {
        try {
            if (cron != null) {
                task.setCron(cron);
            }
            if (task.getId() == null) {
                task.setId(task.getDescription() + UUID.randomUUID().toString());
            }
            QuartzFactory.getInstance().addCronJob(task);
        } catch (Throwable e) {
            e.printStackTrace();
            LoggerEx.error(TAG, "Schedule scheduleTask " + task + " failed, " + e);
        }
    }
}