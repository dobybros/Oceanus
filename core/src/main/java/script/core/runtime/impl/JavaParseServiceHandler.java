package script.core.runtime.impl;

import chat.errors.ChatErrorCodes;
import chat.errors.CoreException;
import chat.logs.LoggerEx;
import org.apache.commons.lang.exception.ExceptionUtils;
import script.core.runtime.AbstractRuntimeContext;
import script.core.runtime.ParseServiceHandler;
import script.core.runtime.classloader.MyJavaClassLoader;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Created by lick on 2019/5/15.
 * Description：
 */
public class JavaParseServiceHandler implements ParseServiceHandler {
    protected static final String TAG = ParseServiceHandler.class.getSimpleName();
    private  static  final String MANIFEST_DIRECTORY_LOCATION = "META-INF" + File.separator +  "MANIFEST.MF";
    private  static  final String MANIFEST_MAIN_CLASS = "Main-Class";
    private  static  final String MAIN_METHOD = "main";
    public void beforeDeploy() {}

    @Override
    public synchronized void start(ClassLoader classLoader) throws CoreException {
        MyJavaClassLoader myJavaClassLoader = (MyJavaClassLoader)classLoader;
        AbstractRuntimeContext runtimeContext = myJavaClassLoader.getRuntimeContext();
        if (runtimeContext == null)
            throw new NullPointerException("runtime is empty while redeploy for Booter " + this);
        LoggerEx.info(TAG, "Will parse path " + runtimeContext.getConfiguration().getLocalPath());
        boot(runtimeContext, myJavaClassLoader);
    }

    private void boot(AbstractRuntimeContext runtimeContext, MyJavaClassLoader javaClassLoader) throws CoreException{
        try {
            File jarFile = new File(runtimeContext.getConfiguration().getLocalPath() + File.separator + runtimeContext.getConfiguration().getFileName());
            Properties properties = manifestReaderFromJar(jarFile);
            Class<?> aClass = javaClassLoader.loadClass(properties.getProperty(MANIFEST_MAIN_CLASS));
            Method main = aClass.getDeclaredMethod(MAIN_METHOD, String[].class);
            synchronized (runtimeContext.getConfiguration().getBaseConfiguration()){
                resetURLFactory();
                main.invoke(null, (Object) new String[]{});
            }
        } catch (Throwable e) {
            throw new CoreException(ChatErrorCodes.ERROR_BOOT, "Start java failed, configuration: " + runtimeContext.getConfiguration() + "errMsg: " + ExceptionUtils.getFullStackTrace(e));
        }
    }

    private Properties manifestReaderFromJar(File file) {
        try (JarFile jar = new JarFile(file)){
            JarEntry entry = jar.getJarEntry(MANIFEST_DIRECTORY_LOCATION);
            if (entry !=  null) {
                Properties properties = new Properties();
                properties.load(jar.getInputStream(entry));
                return properties;
            }
        }  catch (Throwable e) {
            throw new RuntimeException("Cannot read "+ MANIFEST_DIRECTORY_LOCATION + " from jar '" + file.getAbsolutePath() +  "'.", e);
        }
        throw new RuntimeException("Cannot read "+ MANIFEST_DIRECTORY_LOCATION + " from jar " + file.getAbsolutePath());
    }
    private void resetURLFactory() throws NoSuchFieldException, IllegalAccessException {
        final Field factoryField = URL.class.getDeclaredField("factory");
        factoryField.setAccessible(true);
        factoryField.set(null, null);
    }
}
