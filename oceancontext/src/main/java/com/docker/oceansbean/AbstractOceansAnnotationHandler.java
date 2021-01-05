package com.docker.oceansbean;


import chat.base.bean.handler.BaseAnnotationHandler;

/**
 * Created by lick on 2021/1/2.
 * Description：
 */
public abstract class AbstractOceansAnnotationHandler implements BaseAnnotationHandler {
    protected OceanusBeanManager oceanusBeanManager;
    public AbstractOceansAnnotationHandler(OceanusBeanManager oceanusBeanManager){
        this.oceanusBeanManager = oceanusBeanManager;
    }

    public void setOceanusBeanManager(OceanusBeanManager oceanusBeanManager) {
        this.oceanusBeanManager = oceanusBeanManager;
    }
}
