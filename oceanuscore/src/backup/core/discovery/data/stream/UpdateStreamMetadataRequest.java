package core.discovery.data.stream;

import core.net.data.RequestTransport;

import java.util.Map;

public class UpdateStreamMetadataRequest extends RequestTransport<GetStreamMetadataResponse> {
    private String id; //must
    private String name;
    private Long expireTime;
    private Map<String, String> metadata;
    private String executorId;
}
