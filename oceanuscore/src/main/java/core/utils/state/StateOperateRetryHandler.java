package core.utils.state;

import core.common.InternalTools;
import core.log.LoggerHelper;
import script.utils.state.StateMachine;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class StateOperateRetryHandler<K, T> {
    private int retryCount = 0;
    private int MAX_RETRY = 5;
    private long RETRY_INTERVAL = 1000L;
    private ScheduledFuture<?> retryTask;
    private OperateListener<K, T> operateListener, initializingListener;
    private OperateFailedListener<K, T> operateFailedListener;
    private InternalTools internalTools;
    private OperateFailedOccurError<K, T> operateFailedOccurError;
    private StateMachine<K, T> stateMachine;

    private StateOperateRetryHandler(StateMachine<K, T> stateMachine, InternalTools internalTools) {
        this.internalTools = internalTools;
        this.stateMachine = stateMachine;
    }

    public static <K, T> StateOperateRetryHandler<K, T> build(StateMachine<K, T> stateMachine, InternalTools internalTools) {
        return new StateOperateRetryHandler<K, T>(stateMachine, internalTools);
    }

    public StateOperateRetryHandler<K, T> setMaxRetry(int maxRetry) {
        this.MAX_RETRY = maxRetry;
        return this;
    }

    public StateOperateRetryHandler<K, T> setRetryInterval(long retryInterval) {
        this.RETRY_INTERVAL = retryInterval;
        return this;
    }

    // retry的执行
    public StateOperateRetryHandler<K, T> setOperateListener(OperateListener<K, T> operateListener) {
        this.operateListener = operateListener;
        return this;
    }

    // 重置retry状态的回调
    public StateOperateRetryHandler<K, T> setInitializingListener(OperateListener<K, T> initializingListener) {
        this.initializingListener = initializingListener;
        return this;
    }

    // retry失败后的执行方法
    public StateOperateRetryHandler<K, T> setOperateFailedListener(OperateFailedListener<K, T> operateFailedListener) {
        this.operateFailedListener = operateFailedListener;
        return this;
    }

    // 执行retry失败的方法失败后的回调
    public StateOperateRetryHandler<K, T> setOperateFailedOccurError(OperateFailedOccurError<K, T> operateFailedOccurError) {
        this.operateFailedOccurError = operateFailedOccurError;
        return this;
    }

    public void operate(T t, StateMachine<K, T> stateMachine) throws Throwable {
        if(operateListener != null) {
            operateListener.operate(t, stateMachine);
        }
    }

    public void operateFailed(T tt, StateMachine<K, T> stateMachine) {
        if(operateFailedListener != null) {
            releaseRetryTask();
            if(retryTask == null) {
                retryTask = internalTools.getScheduledExecutorService().schedule(() -> {
                    retryCount++;
                    if(retryCount > MAX_RETRY) {
                        try {
                            operateFailedListener.operate(false, retryCount, MAX_RETRY, tt, stateMachine);
                        } catch(Throwable t) {
                            t.printStackTrace();
                            if(operateFailedOccurError != null) {
                                try {
                                    operateFailedOccurError.operate(false, retryCount, MAX_RETRY, t, tt, stateMachine);
                                } catch (Throwable throwable) {
                                    throwable.printStackTrace();
                                    LoggerHelper.logger.error("operateFailedOccurError failed, " + throwable.getMessage());
                                }
                            }
                        }
                    } else {
                        try {
                            operateFailedListener.operate(true, retryCount, MAX_RETRY, tt, stateMachine);
                        } catch (Throwable t) {
                            t.printStackTrace();
                            if(operateFailedOccurError != null) {
                                try {
                                    operateFailedOccurError.operate(true, retryCount, MAX_RETRY, t, tt, stateMachine);
                                } catch (Throwable throwable) {
                                    throwable.printStackTrace();
                                    LoggerHelper.logger.error("operateFailedOccurError failed, " + throwable.getMessage());
                                }
                            }
                        }
                    }
                }, RETRY_INTERVAL, TimeUnit.MILLISECONDS);
            }
        }
    }

    public void initializing(T t, StateMachine<K, T> stateMachine) throws Throwable {
        releaseRetryTask();
        retryCount = 0;
        if(initializingListener != null)
            initializingListener.operate(t, stateMachine);
    }

    private void releaseRetryTask() {
        if(retryTask != null) {
            retryTask.cancel(true);
            retryTask = null;
        }
    }

    public interface OperateListener<K, T> {
        void operate(T t, StateMachine<K, T> stateMachine) throws Throwable;
    }
    public interface OperateFailedListener<K, T> {
        void operate(boolean willRetry, int retryCount, int maxRetry, T t, StateMachine<K, T> stateMachine) throws Throwable;
    }

    public interface OperateFailedOccurError<K, T> {
        void operate(boolean willRetry, int retryCount, int maxRetry, Throwable throwable, T t, StateMachine<K, T> stateMachine) throws Throwable;
    }
}
