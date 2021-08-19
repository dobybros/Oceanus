package core.discovery.data.stream;

import com.alibaba.fastjson.annotation.JSONField;
import core.net.data.RequestTransport;

import java.io.InputStream;
import java.util.Map;

public class UpStreamRequest extends RequestTransport<UpStreamResponse> {
    @JSONField(serialize = false)
    private InputStream inputStream;
    private String name;
    private String id;
    private String executorId;
    private Long expireTime;
    private Map<String, String> metadata;
}
