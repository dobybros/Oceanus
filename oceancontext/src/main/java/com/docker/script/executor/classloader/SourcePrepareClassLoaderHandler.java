package com.docker.script.executor.classloader;

import chat.config.Configuration;
import chat.errors.ChatErrorCodes;
import chat.errors.CoreException;
import chat.logs.LoggerEx;
import com.docker.oceansbean.BeanFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import script.core.runtime.AbstractRuntimeContext;
import script.core.runtime.classloader.ClassLoaderFactory;
import script.core.runtime.classloader.impl.DefaultClassLoaderFactory;
import script.core.runtime.handler.compile.classloader.PrepareClassLoaderHandler;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by lick on 2020/12/21.
 * Description：
 */
public class SourcePrepareClassLoaderHandler implements PrepareClassLoaderHandler {
    private final String TAG = SourcePrepareClassLoaderHandler.class.getSimpleName();
    private ClassLoaderFactory classLoaderFactory = (ClassLoaderFactory) BeanFactory.getBean(DefaultClassLoaderFactory.class.getName());
    private ClassLoader parentClassLoader;
    public SourcePrepareClassLoaderHandler(ClassLoader parentClassLoader){
        this.parentClassLoader = parentClassLoader;
    }
    @Override
    public ClassLoader prepare(AbstractRuntimeContext runtimeContext) throws CoreException {
        Configuration configuration = runtimeContext.getConfiguration();
        switch (configuration.getLanguageType()){
            case Configuration.LANGEUAGE_GROOVY:
                return classLoaderFactory.create(null, parentClassLoader, false, runtimeContext);
            case Configuration.LANGEUAGE_JAVA:
            case Configuration.LANGEUAGE_JAVA_JAR:
                List<URL> urls = new ArrayList<>();
                File sourcePath = new File(runtimeContext.getConfiguration().getLocalPath());
                if (sourcePath.exists() && sourcePath.isDirectory()) {
                    Collection<File> jars = new ArrayList<>(FileUtils.listFiles(sourcePath,
                            FileFilterUtils.suffixFileFilter(".jar"),
                            FileFilterUtils.directoryFileFilter()));
                    jars.forEach(jar -> {
                        String path = "jar:file://" + jar.getAbsolutePath() + "!/";
                        try {
                            urls.add(jar.toURI().toURL());
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                            LoggerEx.warn(TAG, "MalformedURL " + path + " while load jars, error " + e.getMessage());
                        }
                    });
                    if(!urls.isEmpty()){
                        return classLoaderFactory.create(urls.toArray(new URL[0]), parentClassLoader, false, runtimeContext);
                    }
                }
        }
        throw new CoreException(ChatErrorCodes.ERROR_CLASSLOADER_CREATE_FAILED, "Create classloader failed, configuration: " + configuration);
    }
}
