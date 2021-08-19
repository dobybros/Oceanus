package oceanus.services.gatewaymanager.entity;


public class UserInfo {

    private Long time;

    private String server;
    private String host;
    private Integer port;

    private String deviceToken;
    private Integer terminal;

    public static final String FIELD_TIME = "time";

    public static final String FIELD_SERVER = "server";
    public static final String FIELD_HOST = "host";
    public static final String FIELD_PORT = "port";

    public static final String FIELD_DEVICE_TOKEN = "deviceToken";
    public static final String FIELD_TERMINAL = "terminal";

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    public Integer getTerminal() {
        return terminal;
    }

    public void setTerminal(Integer terminal) {
        this.terminal = terminal;
    }
}
