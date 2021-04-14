package core.discovery.data;

import core.net.data.RequestTransport;

public class StringRequest extends RequestTransport<StringResponse> {
    private String request;

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }
}
