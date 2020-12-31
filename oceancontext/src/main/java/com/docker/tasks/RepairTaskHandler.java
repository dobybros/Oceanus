package com.docker.tasks;

import chat.errors.CoreException;
import chat.logs.LoggerEx;
import com.docker.data.RepairData;
import com.docker.server.OnlineServer;
import com.docker.storage.adapters.impl.RepairServiceImpl;
import com.docker.tasks.annotations.RepairTaskListener;
import script.core.runtime.AbstractRuntimeContext;
import com.docker.utils.BeanFactory;
import script.core.runtime.groovy.object.GroovyObjectEx;
import script.core.runtime.handler.annotation.clazz.ClassAnnotationGlobalHandler;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by lick on 2020/4/14.
 * Description：
 */
public class RepairTaskHandler extends ClassAnnotationGlobalHandler {
    private final String TAG = RepairTaskHandler.class.getSimpleName();
    private RepairServiceImpl repairService;
    private Map<String, GroovyObjectEx> groovyObjectExMap = new ConcurrentHashMap<>();

    @Override
    public Class<? extends Annotation> handleAnnotationClass() {
        return RepairTaskListener.class;
    }

    @Override
    public void handleAnnotatedClassesInjectBean(AbstractRuntimeContext runtimeContext) {
        for (GroovyObjectEx groovyObjectEx : groovyObjectExMap.values()) {
            try {
                groovyObjectEx = (GroovyObjectEx) getObject(null, groovyObjectEx.getGroovyClass(), runtimeContext);
            }catch (CoreException e){
                LoggerEx.error(TAG, e.getMessage());
            }
        }
    }

    @Override
    public void handleAnnotatedClasses(Map<String, Class<?>> annotatedClassMap, AbstractRuntimeContext runtimeContext) throws CoreException {
        repairService = (RepairServiceImpl) BeanFactory.getBean(RepairServiceImpl.class.getName());
        LoggerEx.info(TAG, "I will add repair task");
        if (annotatedClassMap != null) {
            Collection<Class<?>> values = annotatedClassMap.values();

            for (Class<?> groovyClass : values) {
                RepairTaskListener repairTaskListener = groovyClass.getAnnotation(RepairTaskListener.class);
                if (repairTaskListener != null) {
                    String description = repairTaskListener.description();
                    String createTime = repairTaskListener.createTime();
                    String id = repairTaskListener.id();
                    int type = repairTaskListener.type();
                    GroovyObjectEx<?> groovyObj = (GroovyObjectEx) getObject(null, groovyClass, runtimeContext);
                        groovyObjectExMap.put(id, groovyObj);
                        try {
                            RepairData repairData = repairService.getRepairData(id);
                            if (repairData == null) {
                                repairData = new RepairData(id, description, createTime, type, "null", "http://" + OnlineServer.getInstance().getIp() + ":" + runtimeContext.getConfiguration().getBaseConfiguration().getServerPort());
                                repairData.setExecuteResult("null");
                                repairService.addRepairData(repairData);
                            } else {
                                repairData.setServerUri("http://" + OnlineServer.getInstance().getIp() + ":" + runtimeContext.getConfiguration().getBaseConfiguration().getServerPort());
                                repairData.setCreateTime(createTime);
                                repairData.setDescription(description);
                                repairData.setType(type);
                                repairService.updateRepairData(repairData);
                            }
                        } catch (Throwable t) {
                            t.printStackTrace();
                            LoggerEx.error(TAG, "Add repairData error, errMsg:" + t.getMessage());
                        }
//                    }
                }
            }
        }
    }

    public Object execute(String id) throws CoreException {
        GroovyObjectEx groovyObjectEx = groovyObjectExMap.get(id);
        if (groovyObjectEx != null) {
            return groovyObjectEx.invokeRootMethod("repair");
        }
        return null;
    }

    public RepairServiceImpl getRepairService() {
        return repairService;
    }
}
