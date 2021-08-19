package com.docker.context;

import oceanus.apis.CoreException;

import java.util.concurrent.CompletableFuture;

public abstract class RPCCaller {
    protected String lanId;

    protected RPCCaller(String lanId) {
        this.lanId = lanId;
    }

    public abstract <T> T call(String service, String className, String method, Class<T> returnClass, Object... args) throws CoreException;

    public abstract <T> T call(String service, String className, String method, String onlyCallOneServer, Class<T> returnClass, Object... args) throws CoreException;

    public abstract <T> CompletableFuture<T> callAsync(String service, String className, String method, Class<T> returnClass, Object... args) throws CoreException;

    public abstract <T> CompletableFuture<T> callAsync(String service, String className, String method, String onlyCallOneServer, Class<T> returnClass, Object... args) throws CoreException;
}
