package core.utils.scheduled;

/**
 * Created by lick on 2019/6/9.
 * Description：
 */
public abstract class AbstractScheduleTask implements Runnable{
    public abstract void execute();

    @Override
    public void run() {
        execute();
    }
}
