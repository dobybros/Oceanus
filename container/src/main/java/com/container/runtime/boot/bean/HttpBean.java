package com.container.runtime.boot.bean;

import chat.base.bean.annotation.OceanusBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;

/**
 * @Auther: lick
 * @Description:
 * @Date:2019/5/26 15:56
 */
@OceanusBean
public class HttpBean{
    private BeanApp instance;
    public HttpBean(){
        instance = BeanApp.getInstance();
    }
//    @Bean(initMethod = "init")
    @OceanusBean
    public PlainSocketFactory plainSocketFactory(){
        return instance.getPlainSocketFactory();
    }
    @OceanusBean
    public SSLSocketFactory sslSocketFactory(){
        return instance.getSslSocketFactory();
    }
    @OceanusBean
    public Scheme httpScheme(){
        return instance.getHttpScheme();
    }
    @OceanusBean
    public Scheme httpsScheme(){
        return instance.getHttpsScheme();
    }
    @OceanusBean
    public SchemeRegistry schemeRegistry(){
        return instance.getSchemeRegistry();
    }
    @OceanusBean
    public ThreadSafeClientConnManager clientConnectionManager(){
        return instance.getClientConnManager();
    }
    @OceanusBean
    public DefaultHttpClient httpClient(){
        return instance.getHttpClient();
    }
}
