package com.scoperetail.simurai.core.application.route.event.bean;

import com.scoperetail.simurai.core.config.SimuraiConfig;
import com.scoperetail.simurai.core.config.camel.Endpoint;
import com.scoperetail.simurai.core.config.camel.EventEndpointMapping;
import lombok.Getter;
import lombok.Setter;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


@Getter
@Setter
@Component
public class RestRoute
{
    @Autowired private CamelContext camelContext;
    @Autowired private SimuraiConfig simuraiConfig;
    @PostConstruct
    public void init() throws Exception
    {
        List<EventEndpointMapping> eventEndpointMappings = simuraiConfig.getEndpointMapping();
        List<Endpoint> endpoints = simuraiConfig.getEndpoints();

        eventEndpointMappings.forEach(

                (endpointInfo)->
                {
                    endpoints.forEach(
                            endpoint ->
                            {
                                if(endpointInfo.getEndpointName().equals(endpoint.getName()))
                                    if (endpoint.getType().equals("GET")||endpoint.getType().equals("POST"))
                                    {
                                        try {
                                            camelContext.addRoutes(new DynamicRestRouteBuilder(camelContext,endpoint,endpoint.getUri()));
                                        } catch (Exception e) {
                                            throw new RuntimeException(e);
                                        }

                                    }
                            }
                    );
                }
        );
        }
    }

class DynamicRestRouteBuilder extends RouteBuilder
{
        private String url;
        private Endpoint endpoint;
        private static final String APPLICATION_JSON = "application/json";
        private static final String CAMEL_REST_COMPONENT = "servlet";

        DynamicRestRouteBuilder(final CamelContext camelContext, Endpoint endpoint, final String url) {
            super(camelContext);
            this.endpoint = endpoint;
            this.url = url;
        }

    @Override
    public void configure() throws Exception
    {
        restConfiguration().component(CAMEL_REST_COMPONENT).bindingMode(RestBindingMode.auto);

        rest(endpoint.getUri()).produces(APPLICATION_JSON).get();
        rest(endpoint.getUri())
                .consumes(APPLICATION_JSON)
                .produces(APPLICATION_JSON)
                .post()
                .type(Map.class)
                .to("direct:triggerEvent");
    }
}
