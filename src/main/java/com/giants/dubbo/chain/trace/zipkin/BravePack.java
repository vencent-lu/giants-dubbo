/**
 * 
 */
package com.giants.dubbo.chain.trace.zipkin;

import zipkin.reporter.AsyncReporter;
import zipkin.reporter.okhttp3.OkHttpSender;

import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.giants.dubbo.chain.trace.zipkin.common.ZipkinConstants;
import com.github.kristofa.brave.Brave;

/**
 * Brave 包装对象，保证单例
 * @author vencent.lu
 *
 */
public class BravePack {
    
    private volatile static BravePack bravePack;
    
    private Brave brave;
    
    private BravePack() {
        this.brave = new Brave.Builder(ConfigUtils.getProperty(ZipkinConstants.APPLICATION_NAME,
                ZipkinConstants.DEFAULT_APPLICATION_NAME)).reporter(
                AsyncReporter.builder(
                        OkHttpSender.create(ConfigUtils.getProperty(ZipkinConstants.ZIPKIN_ADDRESS,
                                ZipkinConstants.DEFAULT_ZIPKIN_ADDRESS))).build()).build();
    }
    
    public static BravePack getInstance() {
        if (bravePack == null) {
            synchronized (BravePack.class) {
                if (bravePack == null) {
                    bravePack = new BravePack();
                }
            }
        }
        return bravePack;
    }

    public Brave getBrave() {
        return brave;
    }

}
