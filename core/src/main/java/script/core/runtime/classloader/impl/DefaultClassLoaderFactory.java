package script.core.runtime.classloader.impl;

import chat.config.Configuration;
import script.Runtime;
import script.core.runtime.AbstractRuntimeContext;
import script.core.runtime.classloader.ClassLoaderFactory;
import script.core.runtime.classloader.DependencyURLClassLoader;
import script.core.runtime.classloader.MyGroovyClassLoader;
import script.core.runtime.classloader.MyJavaClassLoader;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Created by lick on 2020/12/21.
 * Description：
 */
public class DefaultClassLoaderFactory implements ClassLoaderFactory {
    @Override
    public ClassLoader create(URL[] urls, ClassLoader parentClassLoader, boolean isDependency,  AbstractRuntimeContext runtimeContext) {
        if(isDependency){
//            return new DependencyURLClassLoader(urls);
            return new URLClassLoader(urls, this.getClass().getClassLoader().getParent());
        }else {
            switch (runtimeContext.getConfiguration().getLanguageType()){
                case Configuration.LANGEUAGE_GROOVY:
                    return MyGroovyClassLoader.newClassLoader(parentClassLoader, runtimeContext);
                case Configuration.LANGEUAGE_JAVA:
                case Configuration.LANGEUAGE_JAVA_JAR:
                    return new MyJavaClassLoader(urls, parentClassLoader, runtimeContext);
            }
        }
        return null;
    }
}
