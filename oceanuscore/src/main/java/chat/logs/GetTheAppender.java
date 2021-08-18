package chat.logs;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import ch.qos.logback.core.util.OptionHelper;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GetTheAppender {
    public static ConsoleAppender getConsoleAppender() {
        ConsoleAppender consoleAppender = new ConsoleAppender();
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        consoleAppender.setContext(context);
        PatternLayoutEncoder patternLayoutEncoder = new PatternLayoutEncoder();
        patternLayoutEncoder.setPattern("[%thread] %level %logger{36} - %msg%n");
        patternLayoutEncoder.setContext(context);
        patternLayoutEncoder.start();
        consoleAppender.setEncoder(patternLayoutEncoder);
        consoleAppender.setName("console");
        consoleAppender.start();
        return consoleAppender;
    }

    /**
     * 通过传入的名字和级别，动态设置appender
     *
     * @param name
     * @return
     */
    public static RollingFileAppender getAppender(String name) {

        DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
//        format.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        //这里是可以用来设置appender的，在xml配置文件里面，是这种形式：
        // <appender name="error" class="ch.qos.logback.core.rolling.RollingFileAppender">
        RollingFileAppender appender = new RollingFileAppender();

//        //这里设置级别过滤器
//        LevelController levelController = new LevelController();
//        LevelFilter levelFilter = levelController.getLevelFilter(level);
//        levelFilter.start();
//        appender.addFilter(levelFilter);

        //设置上下文，每个logger都关联到logger上下文，默认上下文名称为default。
        // 但可以使用<contextName>设置成其他名字，用于区分不同应用程序的记录。一旦设置，不能修改。
        appender.setContext(context);
        //appender的name属性
        appender.setName("FILE-" + name);
        String path = GetTheAppender.class.getResource("/").toString();
        if (path.startsWith("file:/")) {
            path = path.substring("file:/".length());
            if (!path.startsWith("/") && !path.contains(":/")) {
                path = "/" + path;
            }
        }
        path = Paths.get(path, "../../../logs/").toString();
        String dateStr = format.format(new Date());
        //设置文件名
        appender.setFile(OptionHelper.substVars(path + "/" + "server." + "now" + ".log", context));
//        appender.setFile(OptionHelper.substVars(path + "/" + "server." + dateStr + ".log", context));

        appender.setAppend(true);

        appender.setPrudent(false);

        //设置文件创建时间及大小的类
        SizeAndTimeBasedRollingPolicy policy = new SizeAndTimeBasedRollingPolicy();
        policy.setContext(context);
        //文件名格式
        String fp = OptionHelper.substVars(path + "/" + "server.%d{yyyy-MM-dd}.%i.log", context);
//        String fp = OptionHelper.substVars(path + "/" + "server." + dateStr + "/.%d{yyyy-MM-dd}.%i.log", context);
        //最大日志文件大小
        policy.setMaxFileSize(FileSize.valueOf("1GB"));
        //设置文件名模式
        policy.setFileNamePattern(fp);
        //设置最大历史记录为30条
        policy.setMaxHistory(30);
        //总大小限制
        policy.setTotalSizeCap(FileSize.valueOf("32GB"));
        //设置父节点是appender
        policy.setParent(appender);
        //设置上下文，每个logger都关联到logger上下文，默认上下文名称为default。
        // 但可以使用<contextName>设置成其他名字，用于区分不同应用程序的记录。一旦设置，不能修改。
        policy.setContext(context);
        policy.start();

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        //设置上下文，每个logger都关联到logger上下文，默认上下文名称为default。
        // 但可以使用<contextName>设置成其他名字，用于区分不同应用程序的记录。一旦设置，不能修改。
        encoder.setContext(context);
        //设置格式
        encoder.setPattern("[%thread] %level %logger{35} - %msg%n");
        encoder.start();

        //加入下面两个节点
        appender.setRollingPolicy(policy);
        appender.setEncoder(encoder);
        appender.start();
        return appender;
    }
}
