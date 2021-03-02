package com.dobybros.chat.script.handlers.annotation;

import com.dobybros.chat.open.annotations.RoomStatusListener;

import java.lang.annotation.Annotation;

/**
 * Created by hzj on 2021/1/3 下午5:13
 */
public class RoomStatusAnnotationHandler extends ServiceUserAnnotationHandler {

    public static final String TAG = RoomStatusAnnotationHandler.class.getSimpleName();

    @Override
    public Class<? extends Annotation> handleAnnotationClass() {
        return RoomStatusListener.class;
    }

    @Override
    public String getTag() {
        return TAG;
    }
}
