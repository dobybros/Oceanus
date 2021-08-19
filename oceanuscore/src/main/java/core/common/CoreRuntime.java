package core.common;

import chat.logs.LoggerEx;
import core.utils.scheduled.Timer;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;

import java.util.List;

public abstract class CoreRuntime {
    public static final long FIND_SERVICE_PING_TIMEOUT = 10000L;
    public static final long CONTENT_PACKET_TIMEOUT = 10000L;
    public static final long PERIOD_CLEAN_PING_TIMEOUT = 10000L;
    public static final long SEND_PACKET_TIMEOUT = 10000L;
//    public static final long CONTENT_PACKET_TIMEOUT = 1000000000L;
//    public static final long PERIOD_CLEAN_PING_TIMEOUT = 100000000L;
//    public static final long SEND_PACKET_TIMEOUT = 100000000L;
    private static int cpuCores;
    private static InternalTools internalTools;
    private static HardwareAbstractionLayer hardwareAbstractionLayer;

    private static final String TAG = CoreRuntime.class.getSimpleName();

    static {
        internalTools = new InternalTools();
        internalTools.setTimer(new Timer());
//        internalTools.setScheduledExecutorService(Executors.newScheduledThreadPool(4));

        SystemInfo systemInfo = new SystemInfo();
        hardwareAbstractionLayer = systemInfo.getHardware();
        CentralProcessor centralProcessor = hardwareAbstractionLayer.getProcessor();
        int cores = centralProcessor.getLogicalProcessorCount();
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

    public static List<NetworkIF> getNetworkInterfaces() {
        return hardwareAbstractionLayer.getNetworkIFs(false);
    }

    protected static InternalTools getInternalTools() {
       return internalTools;
    }


}
