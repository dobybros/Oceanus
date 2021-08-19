package oceanus.sdk.server.remote;

import oceanus.sdk.rpc.remote.stub.ServiceStubManager;
import oceanus.sdk.rpc.remote.stub.ServiceStubManagerFactory;
import oceanus.sdk.utils.OceanusProperties;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by lick on 2020/12/22.
 * Description：
 */
public class RuntimeServiceStubManagerFactory implements ServiceStubManagerFactory {
    private final String TAG = RuntimeServiceStubManagerFactory.class.getSimpleName();
    private Map<String, ServiceStubManager> serviceStubManagerMap = new ConcurrentHashMap<>();
    public static final String LAN_ID_DEFAULT = "default";
    public RuntimeServiceStubManagerFactory() {
    }

    @Override
    public ServiceStubManager get(String lanId) {
        String fromService = OceanusProperties.getInstance().getService();

        lanId = LAN_ID_DEFAULT;
        ServiceStubManager serviceStubManager = serviceStubManagerMap.get(lanId);
        if(serviceStubManager == null){
            synchronized (RuntimeServiceStubManagerFactory.class){
                serviceStubManager = serviceStubManagerMap.get(lanId);
                if(serviceStubManager == null){
                    serviceStubManager = new ServiceStubManager(fromService);
                    serviceStubManager.init();
                    serviceStubManagerMap.put(lanId, serviceStubManager);
                }
            }
        }
        return serviceStubManager;
    }

    @Override
    public ServiceStubManager get() {
        return get(null);
    }
}

