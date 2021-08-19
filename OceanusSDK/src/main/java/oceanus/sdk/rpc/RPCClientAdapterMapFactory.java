package oceanus.sdk.rpc;

import oceanus.sdk.logger.LoggerEx;

import java.io.IOException;
import java.util.Properties;

/**
 * Created by lick on 2019/10/15.
 * Description：
 */
public class RPCClientAdapterMapFactory {
    private static final String TAG = RPCClientAdapterMapFactory.class.getSimpleName();
    private static volatile RPCClientAdapterMapFactory instance;
    private RPCClientAdapterMap rpcClientAdapterMap;

    public RPCClientAdapterMap getRpcClientAdapterMap() {
        if(rpcClientAdapterMap == null){
            synchronized (RPCClientAdapterMapFactory.class) {
                if(rpcClientAdapterMap == null){
                    rpcClientAdapterMap = new RPCClientAdapterMap();
                }
            }
        }
        return rpcClientAdapterMap;
    }

    public static synchronized RPCClientAdapterMapFactory getInstance(){
        if(instance == null){
            synchronized (RPCClientAdapterMapFactory.class){
                if(instance == null){
                    instance = new RPCClientAdapterMapFactory();
                }
            }
        }
        return instance;
    }

}
