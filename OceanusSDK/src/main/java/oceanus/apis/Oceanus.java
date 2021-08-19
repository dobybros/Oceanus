package oceanus.apis;

import java.util.concurrent.CompletableFuture;

public interface Oceanus {
    void setNewObjectInterception(NewObjectInterception newObjectInterception);
    CompletableFuture<Void> init(ClassLoader classLoader);
    void injectBean(Object bean);
    RPCManager getRPCManager();

}
