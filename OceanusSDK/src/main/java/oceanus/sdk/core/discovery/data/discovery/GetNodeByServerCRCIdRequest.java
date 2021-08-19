package oceanus.sdk.core.discovery.data.discovery;


import oceanus.sdk.core.net.data.RequestTransport;

public class GetNodeByServerCRCIdRequest extends RequestTransport<GetNodeByServerCRCIdResponse> {
    private Long serverCRCId;

    public Long getServerCRCId() {
        return serverCRCId;
    }

    public void setServerCRCId(Long serverCRCId) {
        this.serverCRCId = serverCRCId;
    }
}
