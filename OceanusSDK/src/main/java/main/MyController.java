package main;

import oceanus.sdk.rpc.remote.annotations.LookupServiceBean;
import oceanus.sdk.rpc.remote.annotations.ServiceBean;

@LookupServiceBean
public class MyController {
    @ServiceBean(name = "goldcentral")
    private CentralService centralService;

    public static MyController instance;
    public MyController() {
        instance = this;
    }

    public CentralService getCentralService() {
        return centralService;
    }

    public void setCentralService(CentralService centralService) {
        this.centralService = centralService;
    }
}
