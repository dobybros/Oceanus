package com.container.runtime.executor.prepare.runtime;

import chat.config.Configuration;
import chat.errors.ChatErrorCodes;
import chat.errors.CoreException;

import com.docker.handler.annotation.field.ConfigPropertyHandler;
import com.docker.handler.annotation.field.JavaBeanHandler;
import com.docker.handler.annotation.field.ServiceBeanHandler;
import com.docker.rpc.remote.skeleton.ServiceSkeletonAnnotationHandler;
import com.docker.script.BaseRuntimeContext;
import com.docker.script.ServiceMemoryHandler;
import com.docker.script.ServiceScaleHandler;
import com.docker.script.executor.prepare.runtime.RuntimeHandler;
import com.docker.script.servlet.GroovyServletManagerEx;
import com.docker.script.servlet.WebServiceAnnotationHandler;
import com.docker.storage.cache.CacheAnnotationHandler;
import com.container.runtime.DefaultRuntimeContext;
import script.Runtime;
import com.docker.oceansbean.BeanFactory;
import script.core.runtime.AbstractRuntimeContext;
import script.core.runtime.RuntimeFactory;
import script.core.runtime.ServerLifeCircleHandler;
import script.core.runtime.handler.AbstractClassAnnotationHandler;
import script.core.runtime.handler.annotation.clazz.*;
import script.core.runtime.impl.DefaultRuntimeFactory;
import script.core.servlets.GroovyServletDispatcher;
import script.core.servlets.RequestPermissionHandler;
import script.filter.JsonFilterFactory;

import java.lang.reflect.InvocationTargetException;

/**
 * Created by lick on 2020/12/18.
 * Description：
 */
public class DefaultRuntimeHandler implements RuntimeHandler {
    private final String TAG = DefaultRuntimeFactory.class.getName();
    @Override
    public BaseRuntimeContext prepare(Configuration configuration) throws CoreException {
        RuntimeFactory runtimeFactory = (RuntimeFactory) BeanFactory.getBean(DefaultRuntimeFactory.class.getName());
        try {
            //创建runtimeContext
            DefaultRuntimeContext runtimeContext = new DefaultRuntimeContext(configuration);
            configuration.getBaseConfiguration().addRuntimeContext(configuration.getService(), runtimeContext);
            prepareAnnotationHandler(runtimeContext);
            //创建runtime
            Runtime runtime = runtimeFactory.create(runtimeContext);
            runtimeContext.setRuntime(runtime);
            //启动编译，以及后续处理
            runtime.start();
            return runtimeContext;
        }catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e){
            AbstractRuntimeContext runtimeContext = configuration.getBaseConfiguration().removeRuntimeContext(configuration.getService());
            if(runtimeContext != null){
                runtimeContext.close();
            }
            throw new CoreException(ChatErrorCodes.ERROR_REFLECT, "Create runtime failed, configuration" + configuration + ",e: " + e.getMessage());
        }
    }
    private void prepareAnnotationHandler(DefaultRuntimeContext runtimeContext){
        if(runtimeContext.getConfiguration().getLanguageType().equals(Configuration.LANGEUAGE_JAVA_JAR))
            return;
        //class's annotations
        String enableGroovyMVC = null;
        runtimeContext.addClassAnnotationHandler(new BeanHandler());
        enableGroovyMVC = runtimeContext.getConfiguration().getConfig().getProperty("web.groovymvc.enable");
//        String mongodbHost = runtimeContext.getConfiguration().getConfig().getProperty("db.mongodb.uri");
//        if (mongodbHost != null) {
//            runtimeContext.addClassAnnotationHandler(new MongoDatabaseAnnotationHolder());
//            runtimeContext.addClassAnnotationHandler(new MongoCollectionAnnotationHolder());
//            runtimeContext.addClassAnnotationHandler(new MongoDocumentAnnotationHolder());
//            MongoDBHandler mongoDBHandler = new MongoDBHandler();
//            MongoClientHelper helper = new MongoClientHelper();
//            helper.setHosts(mongodbHost);
//            mongoDBHandler.setMongoClientHelper(helper);
//            runtimeContext.addClassAnnotationHandler(mongoDBHandler);
//        }

        if (enableGroovyMVC != null && enableGroovyMVC.trim().equals("true")) {
            GroovyServletManagerEx servletManagerEx = new GroovyServletManagerEx(runtimeContext.getConfiguration().getService(), runtimeContext.getConfiguration().getVersion());
            runtimeContext.addClassAnnotationHandler(servletManagerEx);
            GroovyServletDispatcher.addGroovyServletManagerEx(runtimeContext.getConfiguration().getServiceVersion(), servletManagerEx);
            runtimeContext.addClassAnnotationHandler(new WebServiceAnnotationHandler());
        } else {
            GroovyServletDispatcher.removeGroovyServletManagerEx(runtimeContext.getConfiguration().getServiceVersion());
        }

        runtimeContext.addClassAnnotationHandler(new RegisterClassAnnotationHandler());
        runtimeContext.addClassAnnotationHandler(new TimerTaskHandler());
        runtimeContext.addClassAnnotationHandler(new RedeployMainHandler());
        runtimeContext.addClassAnnotationHandler(new ServerLifeCircleHandler());
        runtimeContext.addClassAnnotationHandler(new JsonFilterFactory());
        runtimeContext.addClassAnnotationHandler(new RequestPermissionHandler());
        runtimeContext.addClassAnnotationHandler(new CacheAnnotationHandler());
        runtimeContext.addClassAnnotationHandler(new ServiceMemoryHandler());
        runtimeContext.addClassAnnotationHandler(new ServiceScaleHandler());
        ServiceSkeletonAnnotationHandler serviceSkeletonAnnotationHandler = new ServiceSkeletonAnnotationHandler();
        serviceSkeletonAnnotationHandler.setService(runtimeContext.getConfiguration().getService());
        serviceSkeletonAnnotationHandler.setServiceVersion(runtimeContext.getConfiguration().getVersion());
        /**
         * service's annotations
         */
//        serviceSkeletonAnnotationHandler.addExtraAnnotation(PeriodicTask.class);
//        serviceSkeletonAnnotationHandler.addExtraAnnotation(OneTimeTask.class);
//        serviceSkeletonAnnotationHandler.addExtraAnnotation(KafkaListener.class);
//        serviceSkeletonAnnotationHandler.addExtraAnnotation(Summaries.class);
//        serviceSkeletonAnnotationHandler.addExtraAnnotation(Summary.class);
//        serviceSkeletonAnnotationHandler.addExtraAnnotation(Transactions.class);
//        serviceSkeletonAnnotationHandler.addExtraAnnotation(Transaction.class);
//        serviceSkeletonAnnotationHandler.addExtraAnnotation(TransactionResultNotify.class);
//        serviceSkeletonAnnotationHandler.addExtraAnnotation(ProxyContainerTransportType.class);
        runtimeContext.addClassAnnotationHandler(serviceSkeletonAnnotationHandler);
        runtimeContext.addClassAnnotationHandler((AbstractClassAnnotationHandler) BeanFactory.getBeanByName("dockerRpcServer"));
        runtimeContext.addClassAnnotationHandler((AbstractClassAnnotationHandler) BeanFactory.getBeanByName("dockerRpcServerSsl"));
//        runtimeContext.addClassAnnotationHandler((AbstractClassAnnotationHandler) BeanFactory.getBean(UpStreamAnnotationHandler.class.getName()));
//        runtimeContext.addClassAnnotationHandler((AbstractClassAnnotationHandler) BeanFactory.getBean(RedisSubscribeHandler.class.getName()));
//        runtimeContext.addClassAnnotationHandler((AbstractClassAnnotationHandler) BeanFactory.getBean(RedisListenerHandler.class.getName()));
//        runtimeContext.addClassAnnotationHandler((AbstractClassAnnotationHandler) BeanFactory.getBean(RepairTaskHandler.class.getName()));
//        runtimeContext.addClassAnnotationHandler(new ClassAnnotationHandler() {
//            @Override
//            public void handleAnnotatedClasses(Map<String, Class<?>> annotatedClassMap) throws CoreException {
//                if (annotatedClassMap != null && !annotatedClassMap.isEmpty()) {
//                    StringBuilder uriLogs = new StringBuilder(
//                            "\r\n---------------------------------------\r\n");
//
//                    Set<String> keys = annotatedClassMap.keySet();
//                    for (String key : keys) {
//                        Class<?> groovyClass = annotatedClassMap.get(key);
//                        if (groovyClass != null) {
//                            SessionHandler messageReceivedAnnotation = groovyClass.getAnnotation(SessionHandler.class);
//                            if (messageReceivedAnnotation != null) {
//                                GroovyObjectEx<SessionListener> listeners = (GroovyObjectEx<SessionListener>) getObject(null, groovyClass, runtimeContext);
//                                if (listeners != null) {
//                                    uriLogs.append("ChannelListener #" + groovyClass + "\r\n");
//                                    ((IMRuntimeContext)runtimeContext).setSessionListener(listeners);
//                                    break;
//                                }
//                            }
//                        }
//                    }
//                    uriLogs.append("---------------------------------------");
//                    LoggerEx.info(TAG, uriLogs.toString());
//                }
//            }
//
//            @Override
//            public Class<? extends Annotation> handleAnnotationClass() {
//                return SessionHandler.class;
//            }
//
//        });
//        runtimeContext.addClassAnnotationHandler(new UserStatusAnnotationHandler());
//        runtimeContext.addClassAnnotationHandler(new RoomStatusAnnotationHandler());

        /**
         * field annotations
         */
        runtimeContext.addFieldAnnotationHandler(new ConfigPropertyHandler());
        runtimeContext.addFieldAnnotationHandler(new JavaBeanHandler());
        runtimeContext.addFieldAnnotationHandler(new ServiceBeanHandler());
    }
}
