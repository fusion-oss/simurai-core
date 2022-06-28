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

import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.stereotype.Component;
import com.scoperetail.simurai.core.application.route.event.bean.EventBean;

@Component
public class EventRoute extends RouteBuilder {

  private static final String APPLICATION_JSON = "application/json";
  private static final String CAMEL_REST_COMPONENT = "servlet";

  @Override
  public void configure() throws Exception {

    restConfiguration().component(CAMEL_REST_COMPONENT).bindingMode(RestBindingMode.auto);

    rest("/events").produces(APPLICATION_JSON).get().to("direct:fetchEvents");

    rest("/events")
        .consumes(APPLICATION_JSON)
        .produces(APPLICATION_JSON)
        .post()
        .type(Map.class)
        .to("direct:triggerEvent");

    from("direct:fetchEvents").bean(EventBean.class, "fetchEvents");

    from("direct:triggerEvent")
        .bean(EventBean.class, "triggerEvent")
        .toD("${exchangeProperty.targetUri}")
        .process(
            new Processor() {
              @Override
              public void process(final Exchange exchange) throws Exception {
                //Setting up API response
                exchange.getIn().setBody("Operation is successful");
              }
            });
  }
}
