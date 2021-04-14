package script.core.runtime.classloader;

import script.Runtime;
import script.core.runtime.AbstractRuntimeContext;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;

/**
 * Created by lick on 2020/12/21.
 * Description：
 */
public class MyJavaClassLoader extends URLClassLoader implements DefaultClassLoader {
    private AbstractRuntimeContext runtimeContext;
    public MyJavaClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public MyJavaClassLoader(URL[] urls) {
        super(urls);
    }

    public MyJavaClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
    }

    public MyJavaClassLoader(String name, URL[] urls, ClassLoader parent) {
        super(name, urls, parent);
    }

    public MyJavaClassLoader(String name, URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(name, urls, parent, factory);
    }
    public MyJavaClassLoader(URL[] urls, ClassLoader parent, AbstractRuntimeContext runtimeContext) {
        super(urls, parent);
        this.runtimeContext = runtimeContext;
    }

    @Override
    public AbstractRuntimeContext getRuntimeContext() {
        return runtimeContext;
    }

    @Override
    public ClassHolder getClass(String className) {
        return null;
    }
}
