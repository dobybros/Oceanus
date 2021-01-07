package com.container.runtime.boot.handler;

import chat.errors.CoreException;
import com.container.runtime.boot.manager.DefaultOceansBeanManager;
import org.junit.jupiter.api.Test;

/**
 * Created by lick on 2021/1/2.
 * Description：
 */
class OceansBeanAnnotationHandlerTest {

    @Test
    void handle() throws CoreException {
        OceanusBeanAnnotationHandler oceansBeanAnnotationHandler = new OceanusBeanAnnotationHandler(new DefaultOceansBeanManager());
        oceansBeanAnnotationHandler.handle(null);
    }
}