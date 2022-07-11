package com.scoperetail.simurai.core.application.route.event;

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

import com.scoperetail.simurai.core.config.AMQPBroker;
import com.scoperetail.simurai.core.config.Endpoint;
import com.scoperetail.simurai.core.config.EventEndpointMapping;
import com.scoperetail.simurai.core.config.SimuraiConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.jms.ConnectionFactory;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class SimuraiOrchestratorRoute {
  @Autowired private CamelContext camelContext;
  @Autowired private SimuraiConfig simuraiConfig;
  public static final String mark_regEx = "\\?";
  public static final String equal_regEx = "=";

  @PostConstruct
  public void init() throws Exception {
    List<EventEndpointMapping> enpointMapping = simuraiConfig.getEndpointMapping();
    List<Endpoint> endpoints = simuraiConfig.getEndpoints();

    enpointMapping.forEach(
        (endpointData) -> {
         // Map<String, Object> eventsList = endpointData.getEvents();
          endpoints.forEach(
              endpoint -> {
                if (endpoint.getName().equals(endpointData.getEndpointName())) {
                  if (endpoint.getType().equals("POST") || endpoint.getType().equals("GET")) {
                    try {
                      camelContext.addRoutes(
                          new DynamicRestRouteBuilder(camelContext, endpoint, endpoint.getUri()));
                    } catch (Exception e) {
                      throw new RuntimeException(e);
                    }
                  } else if (endpoint.getType().equals("QUEUE")) {
                    final String url_list[] = endpoint.getUri().split(mark_regEx);
                    final String url = url_list[0];
                    System.out.println(url_list[0] + "url");
                    final String connection_list[] = endpoint.getUri().split(equal_regEx);
                    String brokerUrl = connection_list[1];
                    System.out.println(connection_list[1] + "broker");
                    List<AMQPBroker> brokerslist = simuraiConfig.getAmqpBrokers();
                   /* brokerslist.forEach(
                        e -> {
                          if (brokerUrl.equals(e.getConnectionFactoryName())) {
                              connectionFactory.createConnection(e.getHostUrl());
                          }
                        });*/

                    try {
                      camelContext.addRoutes(
                          new DynamicJMSRouteBuilder(camelContext, endpoint, url, brokerUrl));
                    } catch (Exception e) {
                      throw new RuntimeException(e);
                    }
                  }
                }
              });
        });
  }

  private static final class DynamicRestRouteBuilder extends RouteBuilder {
    private String url;
    private Endpoint endpoint;
    private static final String APPLICATION_JSON = "application/json";
    private static final String CAMEL_REST_COMPONENT = "servlet";

    private DynamicRestRouteBuilder(
        final CamelContext camelContext, Endpoint endpoint, final String url) {
      super(camelContext);
      this.endpoint = endpoint;
      this.url = url;
    }

    @Override
    public void configure() throws Exception {

      restConfiguration().component(CAMEL_REST_COMPONENT).bindingMode(RestBindingMode.auto);

      rest(endpoint.getUri()).produces(APPLICATION_JSON).get();
      System.out.println("REST" + url);
      rest(endpoint.getUri())
          .consumes(APPLICATION_JSON)
          .produces(APPLICATION_JSON)
          .post()
          .type(Map.class)
          .to("direct:triggerEvent");
    }
  }

  private static final class DynamicJMSRouteBuilder extends RouteBuilder {

    private Endpoint endpoint;
    private String url;
    private String brokerUrl;
    @Autowired private SimuraiConfig simuraiConfig;

    public <url> DynamicJMSRouteBuilder(
        final CamelContext camelContext, final Endpoint endpoint, String url, String brokerUrl) {
      super(camelContext);
      this.endpoint = endpoint;
      this.url = url;
      this.brokerUrl = brokerUrl;
    }

    @Override
    public void configure() throws Exception {
      System.out.println(endpoint.getType() + "TYPE " + url + " URLLLL" + brokerUrl + " brokerURL");

      from(url).to("activemq:queue:enduri");
    }
  }

  /*@Bean
  public ActiveMQConnectionFactory connection(String broker) {
    ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory();
    System.out.println("Am inside the Connectionfactory");
    cf.setBrokerURL("http://localhost:8161/admin/");
    cf.setUserName("admin");
    cf.setPassword("admin");
    return cf;
  }
*/

}
