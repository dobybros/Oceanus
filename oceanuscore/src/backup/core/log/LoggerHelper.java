package core.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.rolling.RollingFileAppender;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
public class LoggerHelper {

    private static final Map<String,Logger> container = new HashMap<>();

    public static Logger logger = LoggerHelper.getLogger();
    public static Logger getLogger() {
        return getLogger("server");
    }

    public static Logger getLogger(String name) {
        Logger logger = container.get(name);
        if(logger != null) {
            return logger;
        }
        synchronized (LoggerHelper.class) {
            logger = container.get(name);
            if(logger != null) {
                return logger;
            }
            logger = build(name);
            container.put(name,logger);
        }
        return logger;
    }

    private static Logger build(String name) {
        RollingFileAppender errorAppender = GetTheAppender.getAppender(name,Level.ERROR);
        RollingFileAppender infoAppender = GetTheAppender.getAppender(name,Level.INFO);
        RollingFileAppender warnAppender = GetTheAppender.getAppender(name,Level.WARN);
//        RollingFileAppender debugAppender = GetTheAppender.getAppender(name,Level.DEBUG);
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = context.getLogger("FILE-" + name);
        //设置不向上级打印信息
        logger.setAdditive(false);
        logger.addAppender(errorAppender);
        logger.addAppender(infoAppender);
        logger.addAppender(warnAppender);
//        logger.addAppender(debugAppender);

        logger.addAppender(GetTheAppender.getConsoleAppender());
        return logger;
    }

}
