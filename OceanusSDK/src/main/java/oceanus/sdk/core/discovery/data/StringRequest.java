package oceanus.sdk.core.discovery.data;


import oceanus.sdk.core.net.data.RequestTransport;

public class StringRequest extends RequestTransport<StringResponse> {
    private String request;

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }
}
