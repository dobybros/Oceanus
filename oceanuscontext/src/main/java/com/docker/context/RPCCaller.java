package com.docker.context;

import chat.errors.CoreException;

import java.util.concurrent.CompletableFuture;

public abstract class RPCCaller {
    protected String lanId;

    protected RPCCaller(String lanId) {
        this.lanId = lanId;
    }

    public abstract Object call(String service, String className, String method, Object... args) throws CoreException;

    public abstract Object call(String service, String className, String method, String onlyCallOneServer, Object... args) throws CoreException;

    public abstract CompletableFuture<?> callAsync(String service, String className, String method, Object... args) throws CoreException;

    public abstract CompletableFuture<?> callAsync(String service, String className, String method, String onlyCallOneServer, Object... args) throws CoreException;
}
