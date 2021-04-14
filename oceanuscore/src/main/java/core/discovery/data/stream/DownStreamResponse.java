package core.discovery.data.stream;

import com.alibaba.fastjson.annotation.JSONField;
import core.net.data.ResponseTransport;

import java.io.InputStream;

public class DownStreamResponse extends ResponseTransport {
    @JSONField(serialize = false)
    private InputStream inputStream;

    private String name;
    private String executorId;
    private Long createTime;
    private Long updateTime;
    private Long length;
}
