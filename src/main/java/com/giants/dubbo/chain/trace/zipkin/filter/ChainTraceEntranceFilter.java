/**
 * 
 */
package com.giants.dubbo.chain.trace.zipkin.filter;

import java.io.IOException;
import java.net.URI;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import com.giants.dubbo.chain.trace.zipkin.BravePack;
import com.giants.web.filter.AbstractFilter;
import com.github.kristofa.brave.Brave;
import com.github.kristofa.brave.ServerRequestInterceptor;
import com.github.kristofa.brave.ServerResponseInterceptor;
import com.github.kristofa.brave.http.DefaultSpanNameProvider;
import com.github.kristofa.brave.http.HttpResponse;
import com.github.kristofa.brave.http.HttpServerRequest;
import com.github.kristofa.brave.http.HttpServerRequestAdapter;
import com.github.kristofa.brave.http.HttpServerResponseAdapter;
import com.github.kristofa.brave.http.SpanNameProvider;

/**
 * @author vencent.lu
 *
 */
public class ChainTraceEntranceFilter extends AbstractFilter {
    
    /*private static final String HTTP_SERVER_SPAN_ATTRIBUTE = ChainTraceEntranceFilter.class.getName() + ".server-span";*/
    
    private final ServerRequestInterceptor requestInterceptor;
    private final ServerResponseInterceptor responseInterceptor;
    private final SpanNameProvider spanNameProvider;

    public ChainTraceEntranceFilter() {
        super();
        Brave brave = BravePack.getInstance().getBrave();
        this.requestInterceptor = brave.serverRequestInterceptor();
        this.responseInterceptor = brave.serverResponseInterceptor();
        this.spanNameProvider = new DefaultSpanNameProvider();
    }
    
    /* (non-Javadoc)
     * @see com.giants.web.filter.AbstractFilter#doFilter(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, javax.servlet.FilterChain)
     */
    @Override
    public void doFilter(final HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        /*String service = request.getRequestURI();
        String data = JSON.toJSONString(request.getParameterMap());*/
        final StatusExposingServletResponse statusExposingServletResponse = new StatusExposingServletResponse((HttpServletResponse) response);
        requestInterceptor.handle(new HttpServerRequestAdapter(new HttpServerRequest() {
            
            @Override
            public URI getUri() {
                return URI.create(request.getRequestURI());
            }
            
            @Override
            public String getHttpMethod() {
                return new StringBuilder(request.getMethod()).append(':').append(request.getRequestURI()).toString();
            }
            
            @Override
            public String getHttpHeaderValue(String headerName) {
                return request.getHeader(headerName);
            }
        }, spanNameProvider));
        try {
            chain.doFilter(request, response);
        } finally {
            responseInterceptor.handle(new HttpServerResponseAdapter(new HttpResponse() {
                
                @Override
                public int getHttpStatusCode() {
                    return statusExposingServletResponse.getStatus();
                }
            }));
        }        
    }
    
    private static class StatusExposingServletResponse extends HttpServletResponseWrapper {
        // The Servlet spec says: calling setStatus is optional, if no status is set, the default is OK.
        private int httpStatus = HttpServletResponse.SC_OK;

        public StatusExposingServletResponse(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void sendError(int sc) throws IOException {
            httpStatus = sc;
            super.sendError(sc);
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {
            httpStatus = sc;
            super.sendError(sc, msg);
        }

        @Override
        public void setStatus(int sc) {
            httpStatus = sc;
            super.setStatus(sc);
        }

        @Override
        public int getStatus() {
            return httpStatus;
        }
    }

}
