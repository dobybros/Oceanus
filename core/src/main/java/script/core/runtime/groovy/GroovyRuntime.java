package script.core.runtime.groovy;


import chat.errors.CoreException;
import chat.logs.LoggerEx;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClassRegistry;
import org.apache.commons.lang.exception.ExceptionUtils;
import script.Runtime;
import script.core.runtime.AbstractRuntimeContext;
import script.core.runtime.classloader.MyGroovyClassLoader;
import script.core.runtime.handler.compile.CompileServiceHandler;

public class GroovyRuntime extends Runtime {
    private static final String TAG = GroovyRuntime.class.getName();

    private CompileServiceHandler compileServiceHandler;
    public GroovyRuntime(AbstractRuntimeContext runtimeContext) throws Throwable {
        this.runtimeContext = runtimeContext;
        this.compileServiceHandler = (CompileServiceHandler) Class.forName("com.docker.script.executor.compile.DefaultCompileServiceHandler").getDeclaredConstructor(AbstractRuntimeContext.class).newInstance(runtimeContext);
    }

    @Override
    public synchronized void start() throws CoreException {
        this.compileServiceHandler.compile();
    }
    @Override
    public String path(Class<?> c) {
        return c.getName().replace(".", "/") + ".groovy";
    }

    @Override
    public void close() {
        if (this.runtimeContext.getCurrentClassLoader() != null) {
            MyGroovyClassLoader myGroovyClassLoader = (MyGroovyClassLoader)this.runtimeContext.getCurrentClassLoader();
            try {
                MetaClassRegistry metaReg = GroovySystem
                        .getMetaClassRegistry();
                Class<?>[] classes = myGroovyClassLoader.getLoadedClasses();
                for (Class<?> c : classes) {
                    LoggerEx.info(TAG, this.runtimeContext.getCurrentClassLoader()
                            + " remove meta class " + c);
                    metaReg.removeMetaClass(c);
                }

                myGroovyClassLoader.clearCache();
                myGroovyClassLoader.close();
                LoggerEx.info(TAG, "oldClassLoader " + this.runtimeContext.getCurrentClassLoader()
                        + " is closed");
            } catch (Exception e) {
                e.printStackTrace();
                LoggerEx.error(TAG, this.runtimeContext.getCurrentClassLoader() + " close failed, "
                        + ExceptionUtils.getFullStackTrace(e));
            }
        }
    }


}
