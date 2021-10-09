package chat.logs;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.rolling.RollingFileAppender;
import chat.utils.ChatUtils;
import chat.utils.PropertiesContainer;
import org.slf4j.LoggerFactory;

public class LoggerEx {
    private static Logger logger;
    static {
        RollingFileAppender logAppender = GetTheAppender.getAppender("");
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        logger = context.exists("");
        if(logger == null){
            logger = context.getLogger("");
            //设置不向上级打印信息
            logger.setAdditive(false);
            logger.setLevel(Level.INFO);
            logger.addAppender(logAppender);
            logger.addAppender(GetTheAppender.getConsoleAppender());
        }
    }
    private static final String LEVEL_FATAL = "FATAL";

    private static LogListener logListener;

    private LoggerEx() {
    }

    public interface LogListener {
        void debug(String log);

        void info(String log);

        void warn(String log);

        void error(String log);

        void fatal(String log);
    }

    public static String getClassTag(Class<?> clazz) {
        return clazz.getSimpleName();
    }

    public static void debug(String tag, String msg) {
        String log = getLogMsg(tag, msg);
        if (logListener != null)
            logListener.debug(log);
        else
            logger.debug(log);
    }

    public static void info(String tag, String msg) {
        String log = getLogMsg(tag, msg);
        if (logListener != null)
            logListener.info(log);
        else
            logger.info(log);
    }

    public static void info(String tag, String msg, Long spendTime) {
        String log = getLogMsg(tag, msg, spendTime);
        if (logListener != null)
            logListener.info(log);
        else
            logger.info(log);
    }

    public static void info(String tag, String msg, String dataType, String data) {
        String log = getLogMsg(tag, msg, dataType, data);
        if (logListener != null)
            logListener.info(log);
        else
            logger.info(log);
    }

    public static void warn(String tag, String msg) {
        String log = getLogMsg(tag, msg);
        if (logListener != null)
            logListener.warn(log);
        else
            logger.warn(log);
    }

    public static void error(String tag, String msg) {
        String log = getLogMsg(tag, msg);
        if (logListener != null)
            logListener.error(log);
        else
            logger.error(log);
    }

    public static void fatal(String tag, String msg) {
        String log = getLogMsgFatal(tag, msg);
        if (logListener != null)
            logListener.fatal(log);
        else
            logger.error(log);
    }

    private static String getLogMsg(String tag, String msg) {
        StringBuilder builder = new StringBuilder();
        builder.append("$$time:: " + ChatUtils.dateString()).
                append(" $$tag:: " + tag).
                append(" ").
                append("[" + msg + "]").
                append(" $$env:: " + PropertiesContainer.getInstance().getProperty("lan.id"));

        return builder.toString();
    }

    private static String getLogMsgFatal(String tag, String msg) {
        StringBuilder builder = new StringBuilder();
        builder.append(LEVEL_FATAL).
                append(" $$time:: " + ChatUtils.dateString()).
                append(" $$tag:: " + tag).
                append(" ").
                append("[" + msg + "]").
                append(" $$env:: " + PropertiesContainer.getInstance().getProperty("lan.id"));
        return builder.toString();
    }

    private static String getLogMsg(String tag, String msg, Long spendTime) {
        StringBuilder builder = new StringBuilder();
        builder.append("$$time:: " + ChatUtils.dateString()).
                append(" $$tag:: " + tag).
                append(" [" + msg + "]").
                append(" $$env:: " + PropertiesContainer.getInstance().getProperty("lan.id")).
                append(" $$takes:: " + spendTime);

        return builder.toString();
    }

    private static String getLogMsg(String tag, String msg, String dataType, String data) {
        StringBuilder builder = new StringBuilder();
        builder.append("$$time:: " + ChatUtils.dateString()).
                append(" $$tag:: " + tag).
                append(" [" + msg + "]").
                append(" $$env:: " + PropertiesContainer.getInstance().getProperty("lan.id")).
                append(" $$dataType:: " + dataType).
                append(" $$data:: " + data);

        return builder.toString();
    }

    public static LogListener getLogListener() {
        return logListener;
    }

    public static void setLogListener(LogListener logListener) {
        LoggerEx.logListener = logListener;
    }
}
