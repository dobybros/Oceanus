package core.discovery.data;

import core.net.data.ResponseTransport;

public class StringResponse extends ResponseTransport {
    private String response;

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}
