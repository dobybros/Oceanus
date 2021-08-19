package oceanus.services.gatewaymanager.entity;


public class GatewayServerStatus {
    static final String STATUS_GREEN = "green";
    static final String STATUS_YELLOW = "yellow";
    static final String STATUS_RED = "red";

    private String server;
    private String status;
    private Integer userCount;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public Integer getUserCount() {
        return userCount;
    }

    public void setUserCount(Integer userCount) {
        this.userCount = userCount;
    }
}
