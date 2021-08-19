package oceanus.sdk.apis.impl;

import oceanus.apis.NewObjectInterception;
import oceanus.apis.Oceanus;
import oceanus.apis.RPCManager;
import oceanus.sdk.core.discovery.node.Service;
import oceanus.sdk.logger.LoggerEx;
import oceanus.sdk.rpc.impl.RMIServerHandler;
import oceanus.sdk.rpc.remote.skeleton.LookupServiceBeanAnnotationHandler;
import oceanus.sdk.rpc.remote.skeleton.ServiceSkeletonAnnotationHandler;
import oceanus.sdk.server.OnlineServer;
import oceanus.sdk.utils.OceanusProperties;
import oceanus.sdk.utils.annotation.ClassAnnotationHandler;
import org.apache.commons.lang3.NotImplementedException;
import org.reflections.Reflections;
import org.reflections.scanners.*;
import org.reflections.util.ConfigurationBuilder;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class OceanusImpl implements Oceanus {
    private static final String TAG = OceanusImpl.class.getSimpleName();
    private RPCManager rpcManager;
    private NewObjectInterception newObjectInterception;
    private ConcurrentHashMap<Object, ClassAnnotationHandler> classAnnotationHandlerMap = new ConcurrentHashMap<>();
    private OnlineServer onlineServer;
    private Reflections reflections;
    private AtomicBoolean isStarted = new AtomicBoolean(false);
    public OceanusImpl() {
        OnlineServer onlineServer = OnlineServer.getInstance();
        if(onlineServer != null) {
            throw new IllegalStateException("Oceanus can NOT be initiated twice. ");
        }
        this.onlineServer = new OnlineServer();
        ClassAnnotationHandler[] handlers = new ClassAnnotationHandler[]{
                new ServiceSkeletonAnnotationHandler(),
                new LookupServiceBeanAnnotationHandler(),
        };
        for(ClassAnnotationHandler handler : handlers) {
            classAnnotationHandlerMap.putIfAbsent(handler.getKey(), handler);
        }
        this.onlineServer.setClassAnnotationHandlerMap(classAnnotationHandlerMap);
    }
    @Override
    public void setNewObjectInterception(NewObjectInterception newObjectInterception) {
        this.newObjectInterception = newObjectInterception;
        OnlineServer.getInstance().setNewObjectInterception(newObjectInterception);
    }

    @Override
    public CompletableFuture<Void> init(ClassLoader classLoader) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            if(isStarted.compareAndSet(false, true)) {
                System.setProperty("sun.rmi.transport.tcp.handshakeTimeout", String.valueOf(30000));
                System.setProperty("sun.rmi.transport.tcp.responseTimeout", String.valueOf(TimeUnit.MINUTES.toMillis(1)));
                String serviceStr = OceanusProperties.getInstance().getService();
                Integer rmiPort = OceanusProperties.getInstance().getRpcPort();
                if(rmiPort != -1) {
                    RMIServerHandler rmiServerHandler = new RMIServerHandler();
                    rmiServerHandler.setRmiPort(rmiPort);
                    rmiServerHandler.setServerName(OnlineServer.getInstance().getServer());
                    rmiServerHandler.setIpHolder(OnlineServer.getInstance().getIpHolder());
                    rmiServerHandler.serverStart();
                    LoggerEx.info(TAG, "Oceanus is in Provider/Consumer mode. Can invoke other providers but also able to be a provider. ");
                } else {
                    LoggerEx.info(TAG, "Oceanus is in Consumer mode. Can only invoke other providers but not able to be a provider. ");
                }
                Service service = new Service();
                service.setService(serviceStr);
                service.setStatus(Service.STATUS_DEPLOYED);
                service.setType(Service.TYPE_JAVA);
                service.setVersion(OceanusProperties.getInstance().getVersion());
                service.setMinVersion(0);
                service.setUploadTime(System.currentTimeMillis());
                OnlineServer.getInstance().registerService(service).whenComplete((serviceRuntime, throwable) -> {
                    if(throwable != null) {
                        future.completeExceptionally(throwable);
                        LoggerEx.error(TAG, "Register service " + service);
                    } else {
                        prepareAnnotations(classLoader);

                        future.complete(null);
                    }
                });
            }
        } catch(Throwable t) {
            t.printStackTrace();
            future.completeExceptionally(t);
        }
        return future;
    }

    private void prepareAnnotations(ClassLoader classLoader) {
        reflections = new Reflections(new ConfigurationBuilder()
                .addScanners(new TypeAnnotationsScanner())
                .forPackages(OceanusProperties.getInstance().getScanPackage())
                .addClassLoader(classLoader));

        for(ClassAnnotationHandler classAnnotationHandler : classAnnotationHandlerMap.values()) {
            classAnnotationHandler.setReflections(reflections);
            try {
                classAnnotationHandler.handle();
            } catch (Throwable e) {
                e.printStackTrace();
                LoggerEx.error(TAG, "ClassAnnotationHandler " + classAnnotationHandler + " handle failed, " + e.getMessage());
            }
        }
    }

    @Override
    public void injectBean(Object bean) {
        throw new NotImplementedException();
    }

    @Override
    public RPCManager getRPCManager() {
        return rpcManager;
    }

    public void setRPCManager(RPCManager rpcManager) {
        this.rpcManager = rpcManager;
    }
}
