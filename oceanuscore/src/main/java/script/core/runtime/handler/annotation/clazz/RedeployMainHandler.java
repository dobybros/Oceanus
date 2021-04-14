package script.core.runtime.handler.annotation.clazz;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Map;

import chat.errors.CoreException;
import chat.utils.ConcurrentHashSet;
import script.core.annotation.RedeployMain;
import script.core.runtime.groovy.object.GroovyObjectEx;
import chat.logs.LoggerEx;

public class RedeployMainHandler extends ClassAnnotationHandler {

	private static final String TAG = RedeployMainHandler.class.getSimpleName();
	private ConcurrentHashSet<GroovyObjectEx> redeploySet = new ConcurrentHashSet<>();
	@Override
	public Class<? extends Annotation> handleAnnotationClass() {
		return RedeployMain.class;
	}

	@Override
	public void handleAnnotatedClasses(Map<String, Class<?>> annotatedClassMap) throws CoreException {
		if(annotatedClassMap != null) {
			ConcurrentHashSet<GroovyObjectEx> newRedeploySet = new ConcurrentHashSet<>();
			Collection<Class<?>> values = annotatedClassMap.values();
			for(Class<?> groovyClass : values) {
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
		for(GroovyObjectEx<?> obj : redeploySet) {
			try {
//				if (obj.metaClass.respondsTo(f, "bar")) {
//
//				}
				obj.invokeRootMethod("shutdown");
			} catch (Throwable e) {
				e.printStackTrace();
				LoggerEx.warn(TAG, "Execute redeploy shutdown for " + obj.getClassPath() + " failed, " + e.getMessage());
			}
		}
	}
}
