package com.docker.context;

import chat.errors.CoreException;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class RPCCaller {
    protected String lanId;

    protected RPCCaller(String lanId) {
        this.lanId = lanId;
    }

    public abstract <T> T call(String service, String className, String method, Class<T> returnClass, Object... args) throws CoreException;

    /**
     * 广播
     *
     * @param serviceList
     * @param className
     * @param method
     * @param args
     * @throws CoreException
     */
    public abstract void broadcast(List<String> serviceList, String className, String method, Object... args) throws CoreException;

    public abstract <T> T call(String service, String className, String method, String onlyCallOneServer, Class<T> returnClass, Object... args) throws CoreException;

    public abstract <T> CompletableFuture<T> callAsync(String service, String className, String method, Class<T> returnClass, Object... args) throws CoreException;

    public abstract <T> CompletableFuture<T> callAsync(String service, String className, String method, String onlyCallOneServer, Class<T> returnClass, Object... args) throws CoreException;
}
