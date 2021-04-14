package core.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.filter.LevelFilter;
import ch.qos.logback.core.spi.FilterReply;

public class LevelController {
    /**
     * 通过level设置过滤器
     *
     * @param level
     * @return
     */
    public LevelFilter getLevelFilter(Level level) {
        LevelFilter levelFilter = new LevelFilter();
        levelFilter.setLevel(level);
        levelFilter.setOnMatch(FilterReply.ACCEPT);
        levelFilter.setOnMismatch(FilterReply.DENY);
        return levelFilter;
    }
}