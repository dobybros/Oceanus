package com.container.runtime.executor.prepare.source;

import chat.errors.ChatErrorCodes;
import chat.errors.CoreException;
import chat.config.Configuration;
import com.docker.oceansbean.BeanFactory;
import com.container.runtime.DefaultRuntimeContext;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.model.FileHeader;
import org.apache.commons.io.FileUtils;
import script.file.FileAdapter;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

/**
 * Created by lick on 2020/12/18.
 * Description：
 */
public class DefaultServiceDownloadHandler implements com.docker.script.executor.prepare.source.ServiceDownloadHandler {
    private final String TAG = DefaultServiceDownloadHandler.class.getName();
    @Override
    public Boolean prepare(Configuration configuration) throws CoreException, IOException {
        FileAdapter fileAdapter = (FileAdapter) BeanFactory.getBean(FileAdapter.class.getName());
        //读取fileEntity
        FileAdapter.FileEntity fileEntity
                = fileAdapter.getFileEntity(new FileAdapter.PathEx(getPath(configuration).replace("\\", "/")));
        if(fileEntity == null){
            throw new CoreException(ChatErrorCodes.ERROR_NO_SOURCEFILE, "Failed get source, serviceVersion is " + configuration.getServiceVersion());
        }
        boolean needRedeploy = true;
        DefaultRuntimeContext runtimeContext = (DefaultRuntimeContext) configuration.getBaseConfiguration().getRuntimeContext(configuration.getService());
        if(runtimeContext != null){
            if(runtimeContext.getConfiguration().getLanguageType().equals(Configuration.LANGEUAGE_JAVA_JAR)){
                needRedeploy = false;
            } else {
                if(runtimeContext.getConfiguration().getDeployVersion() != null) {
                    if(runtimeContext.getConfiguration().getDeployVersion() >= fileEntity.getLastModificationTime()) {
                        needRedeploy = false;
                    } else {
                        runtimeContext.close();
                        configuration.getBaseConfiguration().removeRuntimeContext(configuration.getService());
                    }
                }
            }
        }
        if(needRedeploy) {
            configuration.setDeployVersion(fileEntity.getLastModificationTime());
            if(!configuration.getLanguageType().equals(Configuration.LANGEUAGE_JAVA_JAR)) {
                File zipFile = new File(configuration.getLocalPath() + ".zip");
                //删除旧的zip
                FileUtils.deleteQuietly(zipFile);
                //下载zip
                try (OutputStream zipOs = FileUtils.openOutputStream(zipFile)){
                    fileAdapter.readFile(new FileAdapter.PathEx(fileEntity.getAbsolutePath()), zipOs);
                }
                //解压后的文件夹如果之前有文件，就把这些文件删掉
                File localFile = new File(configuration.getLocalPath());
                if(localFile.exists()){
                    File[] files = localFile.listFiles();
                    if(files != null){
                        for (File file : files) {
                            FileUtils.deleteQuietly(file);
                        }
                    }
                }
                //生成密码
                CRC32 crc = new CRC32();
                crc.update(configuration.getFileName().getBytes());
                //解压
                unzip(zipFile, localFile.getAbsolutePath(), String.valueOf(crc.getValue()));
            } else {
                File jarFile = new File(configuration.getLocalPath() + File.separator + configuration.getFileName());
                //删除旧的jar
                FileUtils.deleteQuietly(jarFile);
                //下载zip
                try (OutputStream zipOs = FileUtils.openOutputStream(jarFile)){
                    fileAdapter.readFile(new FileAdapter.PathEx(fileEntity.getAbsolutePath()), zipOs);
                }
            }
        }
        return needRedeploy;
    }

    private String getPath(Configuration configuration){
        if(!configuration.getLanguageType().equals(Configuration.LANGEUAGE_GROOVY)){
            return configuration.getBaseConfiguration().getRemotePath() + configuration.getServiceVersion() + "#" + configuration.getLanguageType() + File.separator + configuration.getFileName();
        }
        return configuration.getBaseConfiguration().getRemotePath() + configuration.getServiceVersion() + File.separator + configuration.getFileName();
    }

    private void unzip(File zipFile, String dir, String password) throws CoreException {
        ZipFile zFile;
        try {
            zFile = new ZipFile(zipFile);
            File destDir = new File(dir);
            if (destDir.isDirectory() && !destDir.exists()) {
                destDir.mkdir();
            }
            if (zFile.isEncrypted()) {
                zFile.setPassword(password.toCharArray());
            }
            zFile.extractAll(dir);
            //TODO 看是否必要写后边的
            List<FileHeader> headerList = zFile.getFileHeaders();
            List<File> extractedFileList = new ArrayList<File>();
            for (FileHeader fileHeader : headerList) {
                if (!fileHeader.isDirectory()) {
                    extractedFileList.add(new File(destDir, fileHeader.getFileName()));
                }
            }
            File[] extractedFiles = new File[extractedFileList.size()];
            extractedFileList.toArray(extractedFiles);
        } catch (Throwable e) {
            throw new CoreException(ChatErrorCodes.ERROR_SOURCE_DWONLOAD_FAILED, "Unzip failed");
        }
    }
}
