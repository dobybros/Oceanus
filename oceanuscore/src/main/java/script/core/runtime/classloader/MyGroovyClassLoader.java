package script.core.runtime.classloader;

import groovy.lang.GroovyClassLoader;
import org.codehaus.groovy.control.CompilerConfiguration;
import script.Runtime;
import script.core.runtime.AbstractRuntimeContext;

public class MyGroovyClassLoader extends GroovyClassLoader implements DefaultClassLoader{
    private long version;
    private AbstractRuntimeContext runtimeContext;
    public MyGroovyClassLoader(ClassLoader parentClassLoader,
                               CompilerConfiguration cc, AbstractRuntimeContext runtimeContext) {
        super(parentClassLoader, cc);
        this.runtimeContext = runtimeContext;
    }

    @Override
    public AbstractRuntimeContext getRuntimeContext() {
        return runtimeContext;
    }

    public long getVersion() {
        return version;
    }

    public String toString() {
        return MyGroovyClassLoader.class.getSimpleName() + "#" + version;
    }
    @Override
    public ClassHolder getClass(String className) {
        if(className.endsWith(".groovy")) {
            className = className.substring(0, className.length() - 7).replace("/", ".");
        }
        return runtimeContext.getCachedClasses().get(className);
    }

    public static MyGroovyClassLoader newClassLoader(ClassLoader parentClassLoader, AbstractRuntimeContext runtimeContext) {
        CompilerConfiguration cc = new CompilerConfiguration();
        cc.setSourceEncoding("utf8");
        cc.setClasspath(runtimeContext.getConfiguration().getLocalPath());
        cc.setRecompileGroovySource(false);
        cc.setMinimumRecompilationInterval(Integer.MAX_VALUE);
        cc.setVerbose(false);
        cc.setDebug(false);
        cc.setParameters(true);

        return new MyGroovyClassLoader(parentClassLoader, cc, runtimeContext);
    }

    public void setVersion(long version) {
        this.version = version;
    }
}