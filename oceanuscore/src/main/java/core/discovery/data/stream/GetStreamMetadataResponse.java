package core.discovery.data.stream;

import core.net.data.ResponseTransport;

import java.util.Map;

public class GetStreamMetadataResponse extends ResponseTransport {
    private String name;
    private Long expireTime;
    private Map<String, String> metadata;
    private String executorId;
    private Long createTime;
    private Long updateTime;
    private Long length;
}
