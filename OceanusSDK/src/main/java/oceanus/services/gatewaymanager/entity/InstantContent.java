package oceanus.services.gatewaymanager.entity;

public abstract class InstantContent {
    public abstract String getContentType();

    public String getCacheTimeKey() {
        return null;
    }
}