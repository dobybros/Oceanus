package script.core.runtime.handler.annotation.clazz;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import chat.errors.CoreException;
import chat.logs.LoggerEx;
import chat.utils.ConcurrentHashSet;
import script.core.annotation.RedeployMain;
import script.core.runtime.groovy.object.GroovyObjectEx;

public class RedeployMainHandler extends ClassAnnotationHandler {

    private static final String TAG = RedeployMainHandler.class.getSimpleName();
    private ConcurrentHashSet<GroovyObjectEx> redeploySet = new ConcurrentHashSet<>();

    @Override
    public Class<? extends Annotation> handleAnnotationClass() {
        return RedeployMain.class;
    }

    @Override
    public void handleAnnotatedClasses(Map<String, Class<?>> annotatedClassMap) throws CoreException {
        if (annotatedClassMap != null) {
            ConcurrentHashSet<GroovyObjectEx> newRedeploySet = new ConcurrentHashSet<>();
            //按order排序
            List<Class<?>> values = annotatedClassMap.values().stream().sorted((c1, c2) -> {
                RedeployMain order1 = c1.getAnnotation(RedeployMain.class);
                RedeployMain order2 = c2.getAnnotation(RedeployMain.class);
                return order1.order() - order2.order();
            }).collect(Collectors.toList());

            for (Class<?> groovyClass : values) {
                GroovyObjectEx<?> groovyObj = (GroovyObjectEx<?>) getObject(null, groovyClass, runtimeContext);
                try {
                    groovyObj.invokeRootMethod("main");
                } catch (Throwable t) {
                    t.printStackTrace();
                    LoggerEx.warn(TAG, "Execute redeploy main for " + groovyClass + " failed, " + t.getMessage());
                }
                newRedeploySet.add(groovyObj);
            }
            redeploySet = newRedeploySet;
        }
    }

    @Override
    public void handlerShutdown() {
        for (GroovyObjectEx<?> obj : redeploySet) {
            try {
                obj.invokeRootMethod("shutdown");
            } catch (Throwable e) {
                LoggerEx.warn(TAG, "Execute redeploy shutdown for " + obj.getClassPath() + " failed, " + e.getMessage());
            }
        }
    }
}
