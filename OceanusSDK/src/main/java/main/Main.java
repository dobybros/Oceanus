package main;

import com.alibaba.fastjson.JSONArray;
import com.google.common.collect.Lists;
import oceanus.apis.*;
import oceanus.sdk.logger.LoggerEx;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class Main {

    public static void main(String... args) throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, CoreException {
        OceanusBuilder oceanusBuilder = new OceanusBuilder();
//        oceanusBuilder.withNewObjectInterception(new NewObjectInterception() {
//            @Override
//            public Object newObject(Class<?> clazz) {
//                return null;
//            }
//        });
        Oceanus oceanus = oceanusBuilder.build();

        oceanus.init(Main.class.getClassLoader()).thenAccept(unused -> {
            RPCManager rpcManager = oceanus.getRPCManager();
//            for(int i = 0; i < 1; i++) {
//                try {
//                    JSONArray players = rpcManager.call("goldplayer", "PlayerService", "getAllPlayers", JSONArray.class, 0, 10, "fadsf");
//                    System.out.println("players " + players);
//                } catch (CoreException e) {
//                    e.printStackTrace();
//                }
//                try {
//                    Thread.sleep(3000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }

            String hello = null;
            try {
                hello = rpcManager.call("goldcentral", "CentralService", "hello", String.class);
            } catch (CoreException e) {
                e.printStackTrace();
            }
            System.out.println("hello " + hello);
//
//            CentralService centralService = rpcManager.getService("goldcentral", CentralService.class);
//            System.out.println("hello2 " + centralService.hello());
//
//            System.out.println("Hello 3 " + MyController.instance.getCentralService().hello());
//
//            PlayerItem pi = new PlayerItem();
//            pi.setItemId("rr");
//            pi.setCount(324L);
//            pi.setUpdateTime(System.currentTimeMillis());
//            List<PlayerItem> list = new ArrayList<>();
//            list.add(pi);
//            System.out.println("Hello 343 " + MyController.instance.getCentralService().getPlayerItems(pi, list));
//
//            try {
//                centralService.error();
//            } catch(Throwable t) {
//                t.printStackTrace();
//            }
//
//            while(true) {
//                try {
//                    Thread.sleep(3000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                try {
//                    System.out.println("aahahahahah = " + centralService.hello());
//
//                    System.out.println("Hello 324234 " + MyController.instance.getCentralService().getPlayerItems(pi, list));
//
////                    centralService.error();
//                } catch (Throwable t) {
//                    t.printStackTrace();
//                }
//            }
        }).exceptionally(throwable -> {
            System.out.println("hello failed " + throwable.getMessage());
            return null;
        });
        synchronized (oceanusBuilder) {
            try {
                oceanusBuilder.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
