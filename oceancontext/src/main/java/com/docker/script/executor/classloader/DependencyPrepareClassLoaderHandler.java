package com.docker.script.executor.classloader;

import chat.logs.LoggerEx;
import com.docker.utils.BeanFactory;
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
 * Description：将依赖包通过URLClassLoader加载
 */
public class DependencyPrepareClassLoaderHandler implements PrepareClassLoaderHandler {
    private final String TAG = DependencyPrepareClassLoaderHandler.class.getName();
    private ClassLoaderFactory classLoaderFactory = (ClassLoaderFactory) BeanFactory.getBean(DefaultClassLoaderFactory.class.getName());
    @Override
    public ClassLoader prepare(AbstractRuntimeContext runtimeContext) {
        List<URL> urls = new ArrayList<>();
        Collection<File> jars = new ArrayList<>();
        if(runtimeContext.getConfiguration().getLocalDependencyLibsPath() != null){
            File libsPomFile = new File(runtimeContext.getConfiguration().getLocalDependencyLibsPath());
            if (libsPomFile.exists() && libsPomFile.isDirectory()) {
                jars.addAll(FileUtils.listFiles(libsPomFile,
                        FileFilterUtils.suffixFileFilter(".jar"),
                        FileFilterUtils.directoryFileFilter()));
            }
            String loadJarsPath = "";
            if(!jars.isEmpty()){
                for (File jar : jars) {
                    String jarPath = jar.getAbsolutePath();
                    boolean canAddJar = true;
                    if(runtimeContext.getConfiguration().getExcludeDependencies() != null){
                        for (int i = 0; i < runtimeContext.getConfiguration().getExcludeDependencies().length; i++) {
                            if(jarPath.contains(runtimeContext.getConfiguration().getExcludeDependencies()[i])){
                                canAddJar = false;
                                break;
                            }
                        }
                    }
                    if (canAddJar) {
                        String path = "jar:file://" + jarPath + "!/";
                        try {
                            urls.add(jar.toURI().toURL());
                            loadJarsPath += jarPath + ";";
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                            LoggerEx.warn(TAG, "MalformedURL " + path + " while load jars, error " + e.getMessage());
                        }
                    }
                }
                LoggerEx.info(TAG, "Loaded jars " + loadJarsPath);
            }

            if (!urls.isEmpty()) {
                return classLoaderFactory.create(urls.toArray(new URL[0]), null, true, runtimeContext);
            }
        }
        return null;
    }
}
