package com.docker.oceansbean;


import chat.base.bean.handler.BaseAnnotationHandler;
import com.docker.oceansbean.OceanusBeanManager;

/**
 * Created by lick on 2021/1/2.
 * Description：
 */
public abstract class AbstractOceansAnnotationHandler implements BaseAnnotationHandler {
    protected com.docker.oceansbean.OceanusBeanManager oceanusBeanManager;
    public AbstractOceansAnnotationHandler(com.docker.oceansbean.OceanusBeanManager oceanusBeanManager){
        this.oceanusBeanManager = oceanusBeanManager;
    }

    public void setOceanusBeanManager(OceanusBeanManager oceanusBeanManager) {
        this.oceanusBeanManager = oceanusBeanManager;
    }
}
