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
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;

import static com.scoperetail.simurai.core.common.constant.CamelComponentConstants.CAMEL_REST_COMPONENT;

@Component
@Slf4j
public class DynamicEndpointRoute {
    @Autowired
    private CamelContext camelContext;
    @Autowired
    private SimuraiConfig simuraiConfig;

    private enum EndpointType {
        GET, POST, QUEUE, TOPIC;
    }

    private enum HttpStatus {
        STATUS_OK(200);

        private final int value;

        HttpStatus(final int newValue) {
            value = newValue;
        }

        private int getValue() {
            return value;
        }
    }


    @PostConstruct
    public void init() throws Exception {
        final List<String> inboundEndpoints = simuraiConfig.getInboundEndpoints();
        camelContext.addRoutes(new MockHttpRoute());
        for (final String inboundEndpoint : inboundEndpoints) {
            final Optional<Endpoint> endpoints = simuraiConfig.getEndpointByName(inboundEndpoint);
            if (endpoints.isPresent()) {
                final String endpointType = endpoints.get().getType();
                if (EndpointType.GET.name().equals(endpointType) || EndpointType.POST.name().equals(endpointType)) {
                    camelContext.addRoutes(new DynamicRestRouteBuilder(camelContext, endpoints.get()));
                } else if (EndpointType.QUEUE.name().equals(endpointType) || EndpointType.TOPIC.name().equals(endpointType)) {
                    camelContext.addRoutes(new DynamicJMSRouteBuilder(camelContext, endpoints.get()));
                }
            }
        }
    }

    private static final class MockHttpRoute extends RouteBuilder {

        @Override
        public void configure() throws Exception {
            from("direct:mockHttp").process((final Exchange exchange) -> {
                log.info("Request URL {}", exchange.getIn().getHeaders().get(Exchange.HTTP_URL));
                log.info("Response Body {}", exchange.getIn().getBody(String.class));
                log.info("Query Params {}", exchange.getIn().getHeaders().get(Exchange.HTTP_QUERY));
                log.info("Path Params {}", exchange.getIn().getHeaders().get(Exchange.HTTP_PATH));
            }).log("Request received").setHeader(Exchange.HTTP_RESPONSE_CODE, constant(HttpStatus.STATUS_OK.getValue()));
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
                            (final Exchange exchange) -> {
                                log.info("Headers: {}", exchange.getIn().getHeaders());
                                log.info("Response Body: {}", exchange.getIn().getBody(String.class));
                            });
        }
    }

    private static final class DynamicRestRouteBuilder extends RouteBuilder {
        private final Endpoint endpoint;

        private DynamicRestRouteBuilder(final CamelContext camelContext, final Endpoint endpoint) {
            super(camelContext);
            this.endpoint = endpoint;
        }

        @Override
        public void configure() throws Exception {

            restConfiguration().component(CAMEL_REST_COMPONENT).bindingMode(RestBindingMode.auto);

            if (EndpointType.GET.name().equals(endpoint.getType())) {
                rest(endpoint.getUri()).get().to("direct:mockHttp");
            } else if (EndpointType.POST.name().equals(endpoint.getType())) {
                rest(endpoint.getUri()).post().to("direct:mockHttp");
            }
        }
    }
}
