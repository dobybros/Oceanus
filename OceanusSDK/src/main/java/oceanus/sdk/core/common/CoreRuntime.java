package oceanus.sdk.core.common;


import oceanus.sdk.logger.LoggerEx;
import java.util.Timer;

public abstract class CoreRuntime {
    public static final long FIND_SERVICE_PING_TIMEOUT = 10000L;
    public static final long CONTENT_PACKET_TIMEOUT = 10000L;
    public static final long PERIOD_CLEAN_PING_TIMEOUT = 10000L;
    public static final long SEND_PACKET_TIMEOUT = 10000L;
//    public static final long CONTENT_PACKET_TIMEOUT = 1000000000L;
//    public static final long PERIOD_CLEAN_PING_TIMEOUT = 100000000L;
//    public static final long SEND_PACKET_TIMEOUT = 100000000L;
    private static final int cpuCores;
    private static final InternalTools internalTools;

    private static final String TAG = CoreRuntime.class.getSimpleName();

    static {
        internalTools = new InternalTools();
        internalTools.setTimer(new Timer());
//        internalTools.setScheduledExecutorService(Executors.newScheduledThreadPool(4));
        String coreSizeStr = System.getProperty("starfish.cpu.core.size", "4");
        int cores = 0;
        try {
            cores = Integer.parseInt(coreSizeStr);
        } catch(Throwable ignored) {}
        if(cores <= 0) {
            cpuCores = 4;
            LoggerEx.warn(TAG, "Failed to detect cpuCore, make it default to 4 cores, wrong cpuCores is " + cores);
        } else {
            cpuCores = cores;
        }
    }

    public static int getCpuCores() {
        return cpuCores;
    }

    protected static InternalTools getInternalTools() {
       return internalTools;
    }


}
