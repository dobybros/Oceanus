package oceanus.apis;

public interface RPCManager {
    <R> R callOneServer(String service, String clazz, String method, String onlyCallOneServer, Class<R> returnClass, Object... args) throws CoreException;

    <R> R call(String service, String clazz, String method, Class<R> returnClass, Object... args) throws CoreException;

    <S> S getService(String service, Class<S> sClass);

    <S> S getService(String service, Class<S> sClass, String onlyCallOneServer);
}