package com.docker.script.executor.compile;

import chat.config.Configuration;
import chat.errors.ChatErrorCodes;
import chat.errors.CoreException;
import com.docker.script.BaseRuntimeContext;
import com.docker.script.executor.classloader.DependencyPrepareClassLoaderHandler;
import com.docker.script.executor.classloader.SourcePrepareClassLoaderHandler;
import org.apache.commons.lang.exception.ExceptionUtils;
import script.core.runtime.AbstractRuntimeContext;
import script.core.runtime.ParseServiceHandler;
import script.core.runtime.classloader.MyGroovyClassLoader;
import script.core.runtime.classloader.MyJavaClassLoader;
import script.core.runtime.handler.compile.CompileServiceHandler;
import script.core.runtime.handler.compile.annotation.HandlerAnnotationHandler;
import script.core.runtime.handler.compile.annotation.impl.DefaultHandlerAnnotationHandler;
import script.core.runtime.handler.compile.classloader.PrepareClassLoaderHandler;

import java.lang.reflect.InvocationTargetException;

/**
 * Created by lick on 2020/12/21.
 * Description：
 */
public class DefaultCompileServiceHandler implements CompileServiceHandler {
    private AbstractRuntimeContext runtimeContext;
    private PrepareClassLoaderHandler dependencyClassLoader;
    private HandlerAnnotationHandler handlerAnnotationHandler;
    public DefaultCompileServiceHandler(AbstractRuntimeContext runtimeContext){
        this.runtimeContext = runtimeContext;
        this.dependencyClassLoader = new DependencyPrepareClassLoaderHandler();
        this.handlerAnnotationHandler = new DefaultHandlerAnnotationHandler();
    }
    @Override
    public void compile() throws CoreException{
        //生成依赖的classloader
        ClassLoader parentClassLoader = this.dependencyClassLoader.prepare(runtimeContext);
        if(parentClassLoader == null){
            if(runtimeContext.getConfiguration().getLanguageType().endsWith(Configuration.LANGEUAGE_JAVA_JAR)){
                parentClassLoader = this.getClass().getClassLoader().getParent();
            }else {
                parentClassLoader = this.getClass().getClassLoader();
            }
        }
        //生成原码的classloader
        ClassLoader sourceClassLoader = new SourcePrepareClassLoaderHandler(parentClassLoader).prepare(runtimeContext);
        runtimeContext.setCurrentClassLoader(sourceClassLoader);
        try {
            ParseServiceHandler parseServiceHandler = null;
            if(sourceClassLoader instanceof MyGroovyClassLoader){
                parseServiceHandler = (ParseServiceHandler) Class.forName("com.docker.script.executor.compile.DefaultGroovyParseServiceHandler").getDeclaredConstructor().newInstance();
            }else if(sourceClassLoader instanceof MyJavaClassLoader){
                parseServiceHandler = (ParseServiceHandler) Class.forName("com.docker.script.executor.compile.DefaultJavaParseServiceHandler").getDeclaredConstructor().newInstance();
            }
            if(parseServiceHandler != null){
                //编译并拿到原码所有class
                parseServiceHandler.start(sourceClassLoader);
            }else {
                throw new CoreException(ChatErrorCodes.ERROR_REFLECT, "parseServiceHandler get failed");
            }
            //处理注解
            this.handlerAnnotationHandler.handle(runtimeContext);
            //对所有bean注入属性
            runtimeContext.injectAllBean();
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | InvocationTargetException | IllegalAccessException e){
            throw new CoreException(ChatErrorCodes.ERROR_REFLECT, ExceptionUtils.getFullStackTrace(e));
        }
    }
}
