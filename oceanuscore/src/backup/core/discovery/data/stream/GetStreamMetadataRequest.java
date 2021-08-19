package core.discovery.data.stream;

import core.net.data.RequestTransport;

public class GetStreamMetadataRequest extends RequestTransport<GetStreamMetadataResponse> {
    private String id;
}
