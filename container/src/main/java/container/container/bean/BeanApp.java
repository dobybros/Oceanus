package container.container.bean;

import com.proxy.runtime.executor.DefaultRuntimeExecutor;
import com.proxy.runtime.ScriptManager;

/**
 * @author lick
 * @date 2019/11/12
 */
public class BeanApp extends ContextBeanApp {
    private static volatile BeanApp instance;
    private ScriptManager scriptManager;

    public synchronized ScriptManager getScriptManager() {
        if (instance.scriptManager == null) {
            instance.scriptManager = new ScriptManager();
            instance.scriptManager.setBaseConfiguration(baseConfiguration);
            instance.scriptManager.setRuntimeExecutor(new DefaultRuntimeExecutor());
        }
        return instance.scriptManager;
    }

    public static BeanApp getInstance() {
        if (instance == null) {
            synchronized (BeanApp.class) {
                if (instance == null) {
                    instance = new BeanApp();
                    ContextBeanApp.getInstance();
                }
            }
        }
        return instance;
    }
}
