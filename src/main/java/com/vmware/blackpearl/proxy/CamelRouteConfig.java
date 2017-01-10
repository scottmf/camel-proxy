/**
 * Copyright (C) 2015 Scott Feldstein
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.vmware.blackpearl.proxy;

import java.util.Map;

import javax.annotation.PreDestroy;

import org.apache.camel.AsyncCallback;
import org.apache.camel.AsyncProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class CamelRouteConfig {
    
    private final Log log = LogFactory.getLog(CamelRouteConfig.class);
    private RouteDefinition routeDefinition;
    @Autowired
    private ProducerTemplate camelTemplate;
    @Autowired
    private ThreadPoolTaskScheduler httpExecutor;
    @Autowired
    private RouteBuilder route;

    @PreDestroy
    public void shutdown() throws Exception {
        log.info("shutdown route=" + routeDefinition);
        if (routeDefinition != null) { 
            route.getContext().removeRouteDefinition(routeDefinition);
            routeDefinition = null;
        }
        log.info("shutdown complete");
    }

    @Bean
    public RouteBuilder route() {
        return new RouteBuilder() {
            public void configure() throws Exception {
                log.info("configuring servlet interceptor");
                routeDefinition = from("servlet:///?matchOnUriPrefix=true").process(getAsyncProcessor());
            }
        };
    }

    // using an synchronous call
    private Processor getProcessor() {
         return new Processor() {
            public void process(final Exchange exchange) throws Exception {
                processExchange(exchange);
            }
         };
    }

    public void processExchange(final Exchange exchange) {
        String opts = "?concurrentConsumers=128&maxConcurrentConsumers=1024&requestTimeout=30000";
        final String method = (String) exchange.getIn().getHeader(Exchange.HTTP_METHOD);
        opts = (method != null && method.equalsIgnoreCase("get")) ? opts + "&deliveryPersistent=false" : opts;
        if (log.isDebugEnabled()) {
            log.debug("(before jms) exchange=" + exchange +
                      ", inHeaders=" + exchange.getIn().getHeaders() +
                      ", inBody=" + exchange.getIn().getBody());
        }
        Exchange result = camelTemplate.send("http4://google.com?bridgeEndpoint=true&amp;throwExceptionOnFailure=false" + opts, exchange);
        Object body = exchange.getOut().getBody();
        Message out = exchange.getOut();
        if (log.isDebugEnabled()) {
            log.debug("(after jms) body=" + body + ", exchange=" + exchange +
                      ", outHeaders=" + out.getHeaders() +
                      ", inHeaders=" + exchange.getIn().getHeaders() +
                      ", inBody=" + exchange.getIn().getBody());
        }
        Exception e = result.getException();
        if (e == null) {
            Map<String, Object> headers = result.getOut().getHeaders();
            out.setBody(result.getOut().getBody());
            out.setHeaders(headers);
        } else {
            log.error("exception from activemq:webRequest: " + e, e);
            out.setBody(e.getMessage());
        }
    }

    // using an async thread pool
    private Processor getAsyncProcessor() {
         return new AsyncProcessor() {
            public void process(final Exchange exchange) throws Exception {
                processExchange(exchange);
            }
            public boolean process(final Exchange exchange, final AsyncCallback callback) {
                httpExecutor.execute(new Runnable() {
                    public void run() {
                        try {
                            process(exchange);
                            callback.done(false);
                        } catch (Exception e) {
                            log.error(e,e);
                        }
                    }
                });
                return false;
            }
         };
    }

}
