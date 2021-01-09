package com.docker.context;

import chat.errors.CoreException;

public abstract class ServiceGenerator {
    protected String lanId;
    protected ServiceGenerator(String lanId) {
        this.lanId = lanId;
    }
    public abstract <T>T getService(String service, Class<T> clazz) throws CoreException;

    public abstract <T>T getService(String service, Class<T> clazz, String onlyCallOneServer) throws CoreException;
}
