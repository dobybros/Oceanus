package oceanus.sdk.rpc.remote.stub;

import oceanus.sdk.core.discovery.node.Node;
import oceanus.sdk.core.discovery.node.ServiceNodeResult;
import oceanus.sdk.logger.LoggerEx;
import oceanus.sdk.server.OnlineServer;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by lick on 2019/6/26.
 * Description：
 */
public class RemoteServersManager implements Runnable {
    private static volatile RemoteServersManager instance;
    private final String TAG = RemoteServersManager.class.getSimpleName();

    private ConcurrentHashMap<Long, Node> nodeMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, ServiceNodesMonitor> serviceServerCRCIdMap = new ConcurrentHashMap<>();

    private ConcurrentHashMap<String, ConcurrentHashMap<String, RemoteServers.Server>> serviceServersMap = new ConcurrentHashMap<>();

    private ScheduledExecutorService scheduledExecutorService;



    public class ServiceNodesMonitor {
        public ServiceNodesMonitor() {
            state = new AtomicInteger(STATE_NONE);
            nodeServerCRCIds = new ArrayList<>();
        }
        public static final int STATE_NONE = 0;
        public static final int STATE_INIT = 10;
        public static final int STATE_INIT_FINISHED = 20;
        private List<Long> nodeServerCRCIds;
        private AtomicInteger state;
        private Long time;

        public Node getNodeByServerCRC(String serverCRC) {
            Long theServerCRC = null;
            try {
                theServerCRC = Long.valueOf(serverCRC);
            } catch(Throwable ignored) {}
            if(theServerCRC != null) {
                return nodeMap.get(theServerCRC);
            }
            return null;
        }
        public Node getNodeByServerCRC(Long serverCRC) {
            return nodeMap.get(serverCRC);
        }

        public List<Long> getNodeServerCRCIds() {
            return nodeServerCRCIds;
        }

        public void setNodeServerCRCIds(List<Long> nodeServerCRCIds) {
            this.nodeServerCRCIds = nodeServerCRCIds;
        }

        public AtomicInteger getState() {
            return state;
        }

        public void setState(AtomicInteger state) {
            this.state = state;
        }

        public Long getTime() {
            return time;
        }

        public void setTime(Long time) {
            this.time = time;
        }
    }

    public RemoteServersManager() {
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.schedule(this, 10L, TimeUnit.SECONDS);
    }
    @Override
    public void run() {
        OnlineServer.getInstance().getNodeRegistrationHandler().getNodesWithServices(serviceServerCRCIdMap.keySet(), nodeMap.keySet(), false).whenComplete((serviceNodeResult, throwable) -> {
            try {
                if(serviceNodeResult != null) {
                    updateServiceNodeResult(serviceNodeResult);
                } else if(throwable != null) {
                    LoggerEx.error(TAG, "Periodic check service nodes failed, " + throwable.getMessage());
                }
            } finally {
                scheduledExecutorService.schedule(this, 10L, TimeUnit.SECONDS);
            }
        });
    }
    private void updateServiceNodeResult(ServiceNodeResult serviceNodeResult) {
        Map<String, List<Node>> serviceNodes = serviceNodeResult.getServiceNodes();
        if(serviceNodes != null) {
//            Set<String> deletedServices = new HashSet<>();
            Collection<String> services = serviceServerCRCIdMap.keySet();
            for(String service : services) {
                if(serviceNodes.containsKey(service)) {
                    ServiceNodesMonitor monitor = serviceServerCRCIdMap.get(service);
                    if(monitor.state.get() == ServiceNodesMonitor.STATE_INIT_FINISHED) {
                        List<Node> nodes = serviceNodes.get(service);
                        if(nodes == null) nodes = new ArrayList<>();
                        List<Long> serverCRCIds = new ArrayList<>();
                        boolean changed = false;
                        for(Node node: nodes) { //遍历服务器返回的节点列表
                            serverCRCIds.add(node.getServerNameCRC());
                            if(!nodeMap.containsKey(node.getServerNameCRC())) {
                                nodeMap.putIfAbsent(node.getServerNameCRC(), node);
                                LoggerEx.info(TAG, "Found node " + node + " for service " + service + " which node will be shared with other services");
                            }
                            if(!changed && !monitor.nodeServerCRCIds.contains(node.getServerNameCRC())) {
                                //有任何一个不一样说明， 服务的节点列表发生了变化
                                changed = true;
                            }
                        }
                        if(!changed && nodes.size() != monitor.nodeServerCRCIds.size()) {
                            changed = true;
                        }
                        if(changed) {
                            LoggerEx.info(TAG, "Service " + service + " node changed from monitor.nodeServerCRCIds " + monitor.nodeServerCRCIds + " to " + serverCRCIds);
                            monitor.nodeServerCRCIds = serverCRCIds;
                            monitor.time = System.currentTimeMillis();
                        }
                    }
                } /*else {
                    deletedServices.add(service);
                }*/
            }
//            for(String deletedService : deletedServices) {
//                serviceServerCRCIdMap.remove(deletedService);
//            }
        }
        Map<String, List<Long>> serviceNodeCRCIds = serviceNodeResult.getServiceNodeCRCIds();
        if(serviceNodeCRCIds != null) {
            //TODO not implemented
            LoggerEx.error(TAG, "serviceNodeResult.getServiceNodeCRCIds() not implemented yet! " + serviceNodeCRCIds);
        }

        List<Long> deadNodes = serviceNodeResult.getDeadNodes();
        if(deadNodes != null && !deadNodes.isEmpty()) {
            for(Long deadNode : deadNodes) {
                nodeMap.remove(deadNode);
            }
            LoggerEx.info(TAG, "Remove dead nodes " + deadNodes);
        }
    }

    public static RemoteServersManager getInstance() {
        if (instance == null) {
            synchronized (RemoteServersManager.class) {
                if (instance == null) {
                    instance = new RemoteServersManager();
                }
            }
        } 
        return instance;
    }


    public Node getNodeByServerCRC(Long serverCRC) {
        return nodeMap.get(serverCRC);
    }

    public ServiceNodesMonitor getServers(String toService) {
        ServiceNodesMonitor monitor = serviceServerCRCIdMap.get(toService);
        return monitor;
    }

    public void initService(String toService) {
        ServiceNodesMonitor serviceNodesMonitor;
        if(!serviceServerCRCIdMap.containsKey(toService)) {
            serviceServerCRCIdMap.putIfAbsent(toService, new ServiceNodesMonitor());
            serviceNodesMonitor = serviceServerCRCIdMap.get(toService);

            final int[] waitLock = new int[0];
            final ServiceNodesMonitor theMonitor = serviceNodesMonitor;
            if(theMonitor.state.compareAndSet(ServiceNodesMonitor.STATE_NONE, ServiceNodesMonitor.STATE_INIT)) {
                LoggerEx.info(TAG, "Current Thread " + Thread.currentThread() + " is getting nodes for service " + toService);
                CompletableFuture<ServiceNodeResult> future = OnlineServer.getInstance().getNodeRegistrationHandler().getNodesWithServices(Arrays.asList(toService), null, false);
                future.thenAccept(serviceNodeResult -> {
                    if(theMonitor.state.get() == ServiceNodesMonitor.STATE_INIT) {
                        List<Long> nodeIds = new ArrayList<>();
                        List<Node> nodes = serviceNodeResult.getServiceNodes().get(toService);
                        if(nodes != null) {
                            for(Node node : nodes) {
                                nodeIds.add(node.getServerNameCRC());
                                if(!nodeMap.containsKey(node.getServerNameCRC())) {
                                    nodeMap.putIfAbsent(node.getServerNameCRC(), node);
                                    LoggerEx.info(TAG, "Found node " + node + " for service " + toService + " which node will be shared with other services");
                                }
                            }
                        }
                        theMonitor.nodeServerCRCIds = nodeIds;
                        LoggerEx.info(TAG, "Found node server CRC ids " + theMonitor.nodeServerCRCIds + " for service " + toService);
                        theMonitor.time = System.currentTimeMillis();
                    } else {
                        LoggerEx.error(TAG, "Init nodes for service " + toService + " state illegal, expect " + ServiceNodesMonitor.STATE_INIT + " but " + theMonitor.state.get());
                    }
                }).exceptionally(throwable -> {
                    throwable.printStackTrace();
                    LoggerEx.error(TAG, "Init nodes for service " + toService + " failed, " + throwable.getMessage());
                    return null;
                }).whenComplete((aVoid, throwable) -> {
                    theMonitor.state.compareAndSet(ServiceNodesMonitor.STATE_INIT, ServiceNodesMonitor.STATE_INIT_FINISHED);
                    synchronized (waitLock) {
                        waitLock.notify();
                    }
                });
                try {
                    synchronized (waitLock) {
                        waitLock.wait(120000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    LoggerEx.error(TAG, "Get nodes for service " + toService + " failed, " + e.getMessage());
                }
                synchronized (theMonitor) {
                    theMonitor.notifyAll();
                    LoggerEx.info(TAG, "Wake up all the threads that wait for service " + toService + " initialized. current " + Thread.currentThread());
                }
            } else {
                synchronized (theMonitor) {
                    if(theMonitor.state.get() == ServiceNodesMonitor.STATE_INIT) {
                        LoggerEx.warn(TAG, "Another thread is getting nodes for service " + toService + ", current thread " + Thread.currentThread() + " will wait...");
                        try {
                            theMonitor.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        LoggerEx.info(TAG, "Waked up thread " + Thread.currentThread() + " after get nodes done for service " + toService);
                    }
                }
            }
        }
    }
}
