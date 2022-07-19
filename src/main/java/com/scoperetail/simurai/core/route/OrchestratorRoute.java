package com.scoperetail.simurai.core.route;

/*-
 * *****
 * simurai-core
 * -----
 * Copyright (C) 2018 - 2022 Scope Retail Systems Inc.
 * -----
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * =====
 */

import com.scoperetail.simurai.core.config.Endpoint;
import com.scoperetail.simurai.core.config.SimuraiConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class OrchestratorRoute {
    @Autowired
    private CamelContext camelContext;
    @Autowired
    private SimuraiConfig simuraiConfig;

    private static final String GET = "GET";
    private static final String POST = "POST";
    private static final String QUEUE = "QUEUE";
    private static final String TOPIC = "TOPIC";
    private static final int STATUS_OK= 200;


    @PostConstruct
    public void init() throws Exception {

        List<String> inboundEndpoints = simuraiConfig.getInboundEndpoints();
        camelContext.addRoutes(new Dummy());
        inboundEndpoints.forEach(
                inboundEndpoint -> {
                    Optional<Endpoint> endpoints = simuraiConfig.getEndpointByName(inboundEndpoint);
                    if (endpoints.isPresent()) {
                        if (endpoints.get().getType().equals(POST)
                                || endpoints.get().getType().equals(GET)) {
                            try {

                                camelContext.addRoutes(new DynamicRestRouteBuilder(camelContext, endpoints.get()));
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        } else if (endpoints.get().getType().equals(QUEUE)
                                || endpoints.get().getType().equals(TOPIC)) {
                            try {
                                camelContext.addRoutes(new DynamicJMSRouteBuilder(camelContext, endpoints.get()));
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                });

    }

    private static final class DynamicRestRouteBuilder extends RouteBuilder {
        private Endpoint endpoint;
        private static final String APPLICATION_JSON = "application/json";
        private static final String CAMEL_REST_COMPONENT = "servlet";

        private DynamicRestRouteBuilder(final CamelContext camelContext, Endpoint endpoint) {
            super(camelContext);
            this.endpoint = endpoint;
        }

        @Override
        public void configure() throws Exception {

            restConfiguration().component(CAMEL_REST_COMPONENT).bindingMode(RestBindingMode.auto);

            if (GET.equals(endpoint.getType())) {
                rest(endpoint.getUri()).produces(APPLICATION_JSON).get().to("direct:getDummy");
            } else if (POST.equals(endpoint.getType())) {
                rest(endpoint.getUri())
                        .consumes(APPLICATION_JSON)
                        .produces(APPLICATION_JSON)
                        .post()
                        .type(Map.class).to("direct:postDummy");
            }
        }
    }

    private static final class Dummy extends RouteBuilder{

        @Override
        public void configure() throws Exception {
            from("direct:getDummy").log("Rest GET call successful").setHeader(Exchange.HTTP_RESPONSE_CODE, constant(STATUS_OK));
            from("direct:postDummy").log("Rest POST call successful").setHeader(Exchange.HTTP_RESPONSE_CODE, constant(STATUS_OK));
        }
    }

    private static final class DynamicJMSRouteBuilder extends RouteBuilder {

        private final Endpoint endpoint;

        private DynamicJMSRouteBuilder(final CamelContext camelContext, final Endpoint endpoint) {
            super(camelContext);
            this.endpoint = endpoint;
        }

        @Override
        public void configure() throws Exception {
            from(endpoint.getUri())
                    .process(
                            new Processor() {
                                @Override
                                public void process(Exchange exchange) {
                                    log.info("Headers:" + exchange.getIn().getHeaders().toString());
                                    log.info("Response Body:" + exchange.getIn().getBody().toString());
                                }
                            });
        }
    }
}
