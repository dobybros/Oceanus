package com.proxy.runtime.executor.prepare.dependency;

import chat.logs.LoggerEx;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import chat.config.Configuration;
import com.docker.script.executor.prepare.dependency.DependencyDownloadHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import script.utils.CmdUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by lick on 2020/12/18.
 * Description：
 */
public class MvnDependencyDownloadHandler implements DependencyDownloadHandler {
    private final String TAG = MvnDependencyDownloadHandler.class.getName();
    private final String MVN_SYMBOL = "AllThisDependencies";
    private final String MVN_SYMBOL_START = "<!--AllThisDependencies";
    private final String MVN_SYMBOL_END = "AllThisDependencies-->";
    @Override
    public void prepare(Configuration configuration) throws Throwable {
        if(configuration.getLanguageType().equals(Configuration.LANGEUAGE_JAVA_JAR))
            return;
        File pomFile = new File(configuration.getLocalPath() + File.separator + "pom.xml");
        if(pomFile.exists()){
            String mvnSettingPath = "";
            String mvnJarsDir = configuration.getBaseConfiguration().getLibsPath();
            if(mvnJarsDir != null){
                mvnSettingPath = "-s " + configuration.getBaseConfiguration().getMavenSettingsPath();
            }else {
                mvnJarsDir = System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository";
            }
            String pomContent = FileUtils.readFileToString(pomFile, "utf-8");
            if(pomContent.contains(MVN_SYMBOL)){
                try {
                    LoggerEx.info(TAG, "maven info: mvn " + mvnSettingPath + " install -DskipTests -f " + FilenameUtils.separatorsToUnix(pomFile.getAbsolutePath()));
                    CmdUtils.execute("mvn " + mvnSettingPath + " install -DskipTests -f " + FilenameUtils.separatorsToUnix(pomFile.getAbsolutePath()));
                } catch (IOException e) {
                    CmdUtils.execute("mvn.cmd " + mvnSettingPath +" install -DskipTests -f " + FilenameUtils.separatorsToUnix(pomFile.getAbsolutePath()));
                }
                int allThisDependenciesIndexStart = pomContent.indexOf(MVN_SYMBOL_START);
                int allThisDependenciesIndexEnd = pomContent.indexOf(MVN_SYMBOL_END);
                String dependencies = pomContent.substring(allThisDependenciesIndexStart + MVN_SYMBOL_START.length(), allThisDependenciesIndexEnd);
                JSONArray allDependencies = JSON.parseArray(dependencies);
                if (allDependencies != null && !allDependencies.isEmpty()) {
                    File libsPath = new File(configuration.getBaseConfiguration().getLocalPath() + File.separator + "pomlibs" + File.separator + System.currentTimeMillis());
                    if (!libsPath.exists()) {
                        libsPath.mkdirs();
                    }
                    configuration.setLocalDependencyLibsPath(libsPath.getAbsolutePath());
                    for (Object o : allDependencies) {
                        if (o instanceof JSONObject) {
                            JSONObject dependency = (JSONObject) o;
                            if (StringUtils.isNotBlank((String) dependency.get("groupId")) && StringUtils.isNotBlank((String) dependency.get("artifactId")) && StringUtils.isNotBlank((String) dependency.get("version"))) {
                                String[] groupDir = ((String) dependency.get("groupId")).split("\\.");
                                String groupDirStr = "";
                                for (String s : groupDir) {
                                    groupDirStr += s + File.separator;
                                }
                                if (groupDirStr != "") {
                                    String jarDir = mvnJarsDir + File.separator + groupDirStr + dependency.get("artifactId") + File.separator + dependency.get("version");
                                    String jarPath = jarDir + File.separator + dependency.get("artifactId") + "-" + dependency.get("version") + ".jar";
                                    FileUtils.copyFileToDirectory(new File(jarPath), new File(libsPath.getAbsolutePath()));
                                }
                            } else {
                                LoggerEx.error(TAG, "The dependency is not illegal, dependency: " + JSON.toJSONString(dependency) + ",path: " + pomFile.getAbsolutePath());
                            }
                        }
                    }
                    LoggerEx.info(TAG, "Base maven jars path: " + mvnJarsDir + ", mvn download dependency success, service: " + configuration.getServiceVersion() + ",pomPath: " + pomFile.getAbsolutePath());
                }
            }
        }
    }
}
