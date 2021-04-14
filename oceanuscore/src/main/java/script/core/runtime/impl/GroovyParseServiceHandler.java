package script.core.runtime.impl;

import chat.errors.ChatErrorCodes;
import chat.errors.CoreException;
import chat.logs.LoggerEx;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import script.RuntimeContext;
import script.core.runtime.AbstractRuntimeContext;
import script.core.runtime.ParseServiceHandler;
import script.core.runtime.classloader.ClassHolder;
import script.core.runtime.classloader.MyGroovyClassLoader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Created by lick on 2019/5/15.
 * Description：
 */
public class GroovyParseServiceHandler implements ParseServiceHandler {
    protected static final String TAG = ParseServiceHandler.class.getSimpleName();

    public void beforeDeploy(AbstractRuntimeContext runtimeContext) {

    }

    @Override
    public synchronized void start(ClassLoader classLoader) throws CoreException {
        MyGroovyClassLoader myGroovyClassLoader = (MyGroovyClassLoader)classLoader;
        AbstractRuntimeContext runtimeContext = myGroovyClassLoader.getRuntimeContext();
        if (runtimeContext == null)
            throw new NullPointerException("runtime is empty while redeploy for Booter " + this);
        String path = runtimeContext.getConfiguration().getLocalPath() + File.separator;
        path = path.replace("\\", "/");
        try {
            beforeDeploy(runtimeContext);
        } catch (Throwable t) {
            LoggerEx.warn(TAG, "beforeDeploy failed, " + ExceptionUtils.getFullStackTrace(t));
        }
        ByteArrayOutputStream baos = null;
        List<File> compileFirstFiles = new CopyOnWriteArrayList<>();
        try {
            File importPath = new File(path + "/config/imports.groovy");
            StringBuilder importBuilder = null;
            if (importPath.isFile() && importPath.exists()) {
                LoggerEx.info(TAG, "Start imports " + FilenameUtils.separatorsToUnix(importPath.getAbsolutePath()));
                String content = FileUtils.readFileToString(importPath, "utf8");
                if (!content.endsWith("//THE END\r\n")) {
                    final CommandLine cmdLine = CommandLine.parse("groovy " + FilenameUtils.separatorsToUnix(importPath.getAbsolutePath()));
                    ExecuteWatchdog watchdog = new ExecuteWatchdog(TimeUnit.MINUTES.toMillis(15));//设置超时时间
                    DefaultExecutor executor = new DefaultExecutor();
                    baos = new ByteArrayOutputStream();
                    executor.setStreamHandler(new PumpStreamHandler(baos, baos));
                    executor.setWatchdog(watchdog);
                    executor.setExitValue(0);//由于ping被到时间终止，所以其默认退出值已经不是0，而是1，所以要设置它
                    int exitValue = executor.execute(cmdLine);
                    final String result = baos.toString().trim();
                    LoggerEx.info(TAG, "import log " + result);

                    importBuilder = new StringBuilder(content);
                    importBuilder.append("\r\n");
                }
            } else {
                String[] strs = new String[]{
                        "package config",
                        "\r\n",
                };
                String content = StringUtils.join(strs, "\r\n");
                FileUtils.writeStringToFile(importPath, content, "utf8");
                importBuilder = new StringBuilder(content);
                importBuilder.append("\r\n");
            }
            File loggerPath = new File(path + "/chat/logs/LoggerEx.groovy");
            compileFirstFiles.add(loggerPath);
            compileFirstFiles.add(importPath);
            Collection<File> files = FileUtils.listFiles(new File(path),
                    FileFilterUtils.suffixFileFilter(".groovy"),
                    FileFilterUtils.directoryFileFilter());

            if (importBuilder != null) {
                //读取文件信息
                String[] libGroovyFiles = null;
                File coreFile = new File(path + "coregroovyfiles");
                if (coreFile.exists()) {
                    try {
                        String libGroovyFilesStr = FileUtils.readFileToString(coreFile, "utf-8");
                        if (libGroovyFilesStr != null) {
                            libGroovyFiles = libGroovyFilesStr.split("\r\n");
                        }
                    } catch (Throwable throwable) {
                        LoggerEx.warn(TAG, "Read core groovy path failed, reason is " + ExceptionUtils.getFullStackTrace(throwable));
                    }
                }
                for (File file : files) {
                    String absolutePath = FilenameUtils.separatorsToUnix(file.getAbsolutePath());
                    int pathPos = absolutePath.indexOf(path);
                    if (pathPos < 0 || absolutePath.endsWith("config/imports.groovy")) {
                        LoggerEx.info(TAG, "Will parse path " + path + " in file " + absolutePath);
                        continue;
                    }
                    String key = absolutePath.substring(pathPos + path.length());
                    boolean ignore = false;
                    if (libGroovyFiles != null) {
                        List libGroovyFilesList = Arrays.asList(libGroovyFiles);
                        if (libGroovyFilesList.contains(key)) {
                            ignore = true;
                        }
                    }
                    if (ignore)
                        continue;
                    int pos = key.lastIndexOf(".");
                    if (pos >= 0) {
                        key = key.substring(0, pos);
                    }
                    key = key.replace("/", ".");

                    importBuilder.append("import ").append(key).append("\r\n");
                }
                importBuilder.append("//THE END\r\n");

                FileUtils.writeStringToFile(importPath, importBuilder.toString(), "utf8");
            }
            for (File file : compileFirstFiles) {
                myGroovyClassLoader.parseClass(file);
            }
            Class[] loadedClasses = myGroovyClassLoader.getLoadedClasses();
            for(Class clazz : loadedClasses){
                runtimeContext.addClass(new ClassHolder(clazz));
            }
        } catch (Throwable t) {
            t.printStackTrace();
            LoggerEx.fatal(TAG,
                    "Redeploy occur unknown error, " + ExceptionUtils.getFullStackTrace(t)
                            + " redeploy aborted!!!");
            if (t instanceof CoreException)
                throw (CoreException) t;
            else {
                LoggerEx.error(TAG, "Groovy unknown error " + ExceptionUtils.getFullStackTrace(t) + ", path: " + runtimeContext.getConfiguration().getLocalPath());
                throw new CoreException(ChatErrorCodes.ERROR_GROOVY_UNKNOWN,
                        "Groovy unknown error " + t.getMessage());
            }
        }
//        finally {
//            IOUtils.closeQuietly(baos);
//            if (deploySuccessfully) {
//                if (oldClassLoader != null) {
//                    TimerEx.schedule(new TimerTaskEx(GroovyBooter.class.getSimpleName()) {
//                        @Override
//                        public void execute() {
//                            LoggerEx.info(TAG, "Old class loader " + oldClassLoader + " is releasing");
//                            try {
//                                MetaClassRegistry metaReg = GroovySystem
//                                        .getMetaClassRegistry();
//                                Class<?>[] classes = oldClassLoader.getLoadedClasses();
//                                for (Class<?> c : classes) {
//                                    LoggerEx.info(TAG, classLoader
//                                            + " remove meta class " + c);
//                                    metaReg.removeMetaClass(c);
//                                }
//
//                                oldClassLoader.clearCache();
//                                oldClassLoader.close();
//                                LoggerEx.info(TAG, "oldClassLoader " + oldClassLoader
//                                        + " is closed");
//                            } catch (Throwable e) {
//                                e.printStackTrace();
//                                LoggerEx.error(TAG, oldClassLoader + " close failed, "
//                                        + ExceptionUtils.getFullStackTrace(e));
//                            }
//                        }
//                    }, TimeUnit.SECONDS.toMillis(60)); //release old class loader after 60 seconds.
//                    LoggerEx.info(TAG, "Old class loader " + oldClassLoader + " will be released after 60 seconds");
//                }
//                long version = latestVersion.incrementAndGet();
//                newClassLoader.setVersion(version);
//                classLoader = newClassLoader;
//            } else {
//                if (newClassLoader != null) {
//                    try {
//                        newClassLoader.clearCache();
//                        newClassLoader.close();
//                        LoggerEx.info(TAG, "newClassLoader " + newClassLoader
//                                + " is closed");
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }
    }
}
