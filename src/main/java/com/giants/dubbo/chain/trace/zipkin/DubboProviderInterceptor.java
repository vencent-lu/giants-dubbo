package com.giants.dubbo.chain.trace.zipkin;

import static com.github.kristofa.brave.IdConversion.convertToLong;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.giants.common.exception.BusinessException;
import com.github.kristofa.brave.Brave;
import com.github.kristofa.brave.KeyValueAnnotation;
import com.github.kristofa.brave.ServerRequestAdapter;
import com.github.kristofa.brave.ServerRequestInterceptor;
import com.github.kristofa.brave.ServerResponseAdapter;
import com.github.kristofa.brave.ServerResponseInterceptor;
import com.github.kristofa.brave.SpanId;
import com.github.kristofa.brave.TraceData;

/**
 * dubbo服务提供者拦截器
 * @author vencent.lu
 *
 */
@Activate(group = Constants.PROVIDER)
public class DubboProviderInterceptor implements Filter{
	
    private final ServerRequestInterceptor serverRequestInterceptor;
    private final ServerResponseInterceptor serverResponseInterceptor;
    
	public DubboProviderInterceptor() {
	    Brave brave = BravePack.getInstance().getBrave();
        this.serverRequestInterceptor = brave.serverRequestInterceptor();
        this.serverResponseInterceptor = brave.serverResponseInterceptor();
    }

	public Result invoke(Invoker<?> arg0, Invocation arg1) throws RpcException {
		serverRequestInterceptor.handle(new DrpcServerRequestAdapter(arg1));
		Result result ;
		try {
			result =  arg0.invoke(arg1);
			 serverResponseInterceptor.handle(new GrpcServerResponseAdapter(result));
        } finally {
            
        }
		return result;
	}

	static final class DrpcServerRequestAdapter implements ServerRequestAdapter {
    	private Invocation invocation;
        DrpcServerRequestAdapter(Invocation invocation) {
            this.invocation = invocation;
        }

     
        public TraceData getTraceData() {
        	//Random randoml = new Random();
        	Map<String,String> at = this.invocation.getAttachments();
            String sampled = at.get("Sampled");
            String parentSpanId = at.get("ParentSpanId");
            String traceId = at.get("TraceId");
            String spanId = at.get("SpanId");

            // Official sampled value is 1, though some old instrumentation send true
            Boolean parsedSampled = sampled != null
                ? sampled.equals("1") || sampled.equalsIgnoreCase("true")
                : null;

            if (traceId != null && spanId != null) {
                return TraceData.create(getSpanId(traceId, spanId, parentSpanId, parsedSampled));
            } else if (parsedSampled == null) {
                return TraceData.EMPTY;
            } else if (parsedSampled.booleanValue()) {
                // Invalid: The caller requests the trace to be sampled, but didn't pass IDs
                return TraceData.EMPTY;
            } else {
                return TraceData.NOT_SAMPLED;
            }
        }

       
        public String getSpanName() {
            return new StringBuilder(this.invocation.getInvoker().getInterface().getName()).append('.')
                    .append(this.invocation.getMethodName()).toString();
        }

        
        @SuppressWarnings("unused")
        public Collection<KeyValueAnnotation> requestAnnotations() {
            SocketAddress socketAddress = null;
            if (socketAddress != null) {
                KeyValueAnnotation remoteAddrAnnotation = KeyValueAnnotation.create(
                    "DRPC_REMOTE_ADDR", socketAddress.toString());
                return Collections.singleton(remoteAddrAnnotation);
            } else {
                return Collections.emptyList();
            }
        }
    }

    static final class GrpcServerResponseAdapter implements ServerResponseAdapter {

        final Result result;

        public GrpcServerResponseAdapter(Result result) {
            this.result = result;
        }

        public Collection<KeyValueAnnotation> responseAnnotations() {
            if (!result.hasException()) {
                return Collections.<KeyValueAnnotation>emptyList();
            } else {
                if (result.getException() instanceof BusinessException) {
                    return Collections.singletonList(KeyValueAnnotation.create("result", result.getException().getMessage()));
                } else {
                    String errorValue = result.getException().getMessage();
                    if (errorValue == null) {
                        errorValue = result.getException().toString();
                    }
                    return Collections.singletonList(KeyValueAnnotation.create("error", errorValue));
                }                
            }
        }

    }

    static SpanId getSpanId(String traceId, String spanId, String parentSpanId, Boolean sampled) {
        return SpanId.builder()
            .traceIdHigh(traceId.length() == 32 ? convertToLong(traceId, 0) : 0)
            .traceId(convertToLong(traceId))
            .spanId(convertToLong(spanId))
            .sampled(sampled)
            .parentId(parentSpanId == null ? null : convertToLong(parentSpanId)).build();
    }
}