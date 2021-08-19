package oceanus.apis;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLClassLoader;

public class OceanusBuilder {
    private NewObjectInterception newObjectInterception;
    private ClassLoader classLoader;
    private OceanusExternalJarLoader oceanusExternalJarLoader;

    public OceanusBuilder withOceanusExternalJar(String url) {
        this.oceanusExternalJarLoader = new OceanusExternalJarLoader(url);
        return this;
    }
    public OceanusBuilder withNewObjectInterception(NewObjectInterception newObjectInterception) {
        this.newObjectInterception = newObjectInterception;
        return this;
    }
    public Oceanus build() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        if(oceanusExternalJarLoader != null) {
            oceanusExternalJarLoader.loadExternalJar();
        }
        if(classLoader == null) {
            classLoader = OceanusBuilder.class.getClassLoader();
        }
        String oceanusClass = "oceanus.sdk.apis.impl.OceanusImpl";
        String rpcManagerClass = "oceanus.sdk.apis.impl.RPCManagerImpl";
        Class<?> theOceanusClass, theRPCManagerClass;
        theOceanusClass = classLoader.loadClass(oceanusClass);
        theRPCManagerClass = classLoader.loadClass(rpcManagerClass);
        RPCManager rpcManager = (RPCManager) theRPCManagerClass.getConstructor().newInstance();
        Oceanus oceanus = (Oceanus) theOceanusClass.getConstructor().newInstance();
        Method setRPCManagerMethod = theOceanusClass.getMethod("setRPCManager", RPCManager.class);
        setRPCManagerMethod.invoke(oceanus, rpcManager);
        Method setNewObjectInterceptionMethod = theOceanusClass.getMethod("setNewObjectInterception", NewObjectInterception.class);
        setNewObjectInterceptionMethod.invoke(oceanus, this.newObjectInterception);
        return oceanus;
    }
}
