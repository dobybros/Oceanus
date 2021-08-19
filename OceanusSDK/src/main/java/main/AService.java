package main;

import oceanus.sdk.rpc.remote.annotations.RemoteService;

@RemoteService
public class AService {
    public String hello(String str) {
        return "hello world: " + str;
    }
}
