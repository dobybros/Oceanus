package chat.utils;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by lick on 2020/11/18.
 * Description：
 */
public class SingleThreadQueueEx<T> implements Runnable {
    private ConcurrentLinkedQueue<T> queue = new ConcurrentLinkedQueue<>();
    private ThreadPoolExecutor threadPoolExecutor;
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private Handler<T> handler;
    public SingleThreadQueueEx(ThreadPoolExecutor threadPoolExecutor, Handler<T> handler){
        this.threadPoolExecutor = threadPoolExecutor;
        this.handler = handler;
    }
    private void start(){
        if(isRunning.compareAndSet(false, true)){
            threadPoolExecutor.execute(this);
        }
    }
    @Override
    public void run() {
        if(queue != null){
            boolean end = false;
            while (!end){
                if(queue.isEmpty()){
                    synchronized (this){
                        if(queue.isEmpty()){
                            isRunning.compareAndSet(true, false);
                            end = true;
                        }
                    }
                }else {
                    T t = queue.poll();
                    if(t != null){
                        try {
                            this.handler.execute(t);
                        }catch (Throwable e){
                            this.handler.error(t, e);
                        }
                    }
                }
            }
        }
    }
    public void offerAndStart(T t){
        if(queue.isEmpty()){
            synchronized (this){
                queue.add(t);
            }
        }else {
            queue.add(t);
        }
        start();
    };
    public interface Handler<T>{
        void execute(T t);
        void error(T t, Throwable e);
    }
}
