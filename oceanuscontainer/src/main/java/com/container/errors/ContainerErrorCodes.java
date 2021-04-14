package com.container.errors;

/**
 * @author lick
 * @date 2019/11/12
 */
public interface ContainerErrorCodes {
    int START = 11000;
    int ERROR_SERVICE_VERSIONS_CREATE_REMOTE_PATH_FAILED = START + 1;
    int ERROR_SERVICE_VERSIONS_GROOVY_ZIP_PATH_ILLEGAL = START + 2;
    int ERROR_SERVICE_VERSIONS_GROOVY_ZIP_VERSION_ILLEGAL = START + 3;
}
