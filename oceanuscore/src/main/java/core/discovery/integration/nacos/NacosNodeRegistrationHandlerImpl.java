package core.discovery.integration.nacos;

import chat.logs.LoggerEx;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Cluster;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.healthcheck.AbstractHealthChecker;
import core.discovery.NodeRegistrationHandler;
import core.discovery.impl.client.ServiceRuntime;
import core.discovery.node.Node;
import core.discovery.node.Service;
import core.discovery.node.ServiceNodeResult;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class NacosNodeRegistrationHandlerImpl extends NodeRegistrationHandler {
    private static final String TAG = NacosNodeRegistrationHandlerImpl.class.getSimpleName();
    private NamingService naming;

    @Override
    public CompletableFuture<NodeRegistrationHandler> startNode(String discoveryHosts) {
        if(naming == null) {
            synchronized (this) {
                if(naming == null) {
                    try {
                        naming = NamingFactory.createNamingService(discoveryHosts);
                    } catch (NacosException e) {
                        e.printStackTrace();
                        LoggerEx.error(TAG, "");
                    }
                }
            }
        }

        Instance instance = new Instance();
        instance.setIp("55.55.55.55");
        instance.setPort(9999);
        instance.setHealthy(false);
        instance.setWeight(2.0);
        Map<String, String> instanceMeta = new HashMap<>();
        instanceMeta.put("site", "et2");
        instance.setMetadata(instanceMeta);

        com.alibaba.nacos.api.naming.pojo.Service service = new com.alibaba.nacos.api.naming.pojo.Service("nacos.test.4");
        service.setApp("nacos-naming");
        service.sethealthCheckMode("server");
        service.setEnableHealthCheck(true);
        service.setProtectThreshold(0.8F);
        service.setGroup("CNCF");
        Map<String, String> serviceMeta = new HashMap<>();
        serviceMeta.put("symmetricCall", "true");
        service.setMetadata(serviceMeta);
        instance.setService(service);

        Cluster cluster = new Cluster();
        cluster.setName("TEST5");
        cluster.setHealthChecker(healthChecker);
        Map<String, String> clusterMeta = new HashMap<>();
        clusterMeta.put("xxx", "yyyy");
        cluster.setMetadata(clusterMeta);

        instance.setCluster(cluster);
        RPCManager
        naming.registerInstance("nacos.test.4", instance);
        return null;
    }

    @Override
    public void stopNode() {

    }

    @Override
    public CompletableFuture<ServiceRuntime> registerService(Service service) {

        return null;
    }

    @Override
    public CompletableFuture<ServiceNodeResult> getNodesWithServices(Collection<String> services, Collection<Long> checkNodesAvailability, boolean onlyNodeServerCRC) {
        return null;
    }

    @Override
    public CompletableFuture<Node> getNodeByServerCRCId(Long serverCRCId) {
        return null;
    }

    @Override
    public NodeRegistrationHandler unregisterService(String service) {
        return null;
    }

    @Override
    public NodeRegistrationHandler watchNodeEventsForPublicServices(List<String> services, NodeEventListener nodeEventListener) {
        return null;
    }

    @Override
    public NodeRegistrationHandler unwatchNodeEventsForPublicServices(List<String> services, NodeEventListener nodeEventListener) {
        return null;
    }
}
