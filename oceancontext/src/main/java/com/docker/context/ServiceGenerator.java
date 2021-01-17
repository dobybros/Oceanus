package com.docker.context;

import chat.errors.CoreException;

public abstract class ServiceGenerator {
    protected String lanId;
    protected ServiceGenerator(String lanId) {
        this.lanId = lanId;
    }
    public abstract <T>T getService(String service, Class<T> clazz) throws CoreException;

    public abstract <T>T getService(String service, Class<T> clazz, String onlyCallOneServer) throws CoreException;

    public abstract Object call(String service, String className, String method, Object... args) throws CoreException;

    public abstract Object callOneServer(String service, String className, String method, String onlyCallOneServer, Object... args) throws CoreException;
}
