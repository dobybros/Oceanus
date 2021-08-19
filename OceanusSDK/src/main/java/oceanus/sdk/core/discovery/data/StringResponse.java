package oceanus.sdk.core.discovery.data;


import oceanus.sdk.core.net.data.ResponseTransport;

public class StringResponse extends ResponseTransport {
    private String response;

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}
