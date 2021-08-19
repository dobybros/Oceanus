package oceanus.sdk.integration;

public class OceanusInternalIntegration {
    public static MethodStringInvocationListener methodStringInvocationListener;

    public interface MethodStringInvocationListener {
        Object invoke(Object target, String method, Object[] args);
    }
}
