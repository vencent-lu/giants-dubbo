package com.giants.dubbo.chain.trace.zipkin.common;

/**
 * zipkin配置常量
 * @author vencent.lu
 *
 */
public class ZipkinConstants{
    public static final String ZIPKIN_ADDRESS = "dubbo.chain.trace.zipkin.address";
    public static final String DEFAULT_ZIPKIN_ADDRESS = "http://127.0.0.1:9411/api/v1/spans";
    
    public static final String APPLICATION_NAME = "dubbo.application.name";
    public static final String DEFAULT_APPLICATION_NAME = "default-dubbo-application";
}
