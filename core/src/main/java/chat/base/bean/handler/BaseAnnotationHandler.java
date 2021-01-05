package chat.base.bean.handler;

import chat.errors.CoreException;

/**
 * Created by lick on 2021/1/2.
 * Description：
 */
public interface BaseAnnotationHandler {
    public void handle() throws CoreException;

    public Class getAnnotationClass();
}
