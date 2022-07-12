package com.scoperetail.simurai.core.application.route.event.bean;

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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

//import com.scoperetail.simurai.core.common.util.*;
import com.scoperetail.simurai.core.config.*;
import com.scoperetail.simurai.core.config.camel.Endpoint;
import com.scoperetail.simurai.core.config.camel.Event;
import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Autowired;
import com.scoperetail.simurai.core.application.service.transform.impl.DomainToFtlTemplateTransformer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EventBean {

  private static final String TARGET_URI = "targetUri";
  private static final String TRANSFORMER_FTL = "transformer.ftl";
  private static final String HEADER_FTL = "header.ftl";
  private static final String FILE_COMPONENT = "file:///";
  @Autowired private SimuraiConfig simuraiConfig;
  @Autowired private RestRoute restRoute;

  @Autowired private DomainToFtlTemplateTransformer domainToFtlTemplateTransformer;

  public void fetchEvents(final Exchange exchange) throws Exception {
    log.debug("Request received to fetch the events");
    final List<Event> events = simuraiConfig.getEvents();
    for (final Event event : events) {
      final Path headerTemplatePath =
              Paths.get(simuraiConfig.getResourceDirectory(), event.getAlias(), HEADER_FTL);
      final Path bodyTemplatePath =
              Paths.get(simuraiConfig.getResourceDirectory(), event.getAlias(), TRANSFORMER_FTL);
      if (Files.exists(headerTemplatePath) && Files.exists(bodyTemplatePath)) {
        final String eventHeaderData = new String(Files.readAllBytes(headerTemplatePath));
        event.setHeaderTemplate(eventHeaderData);
        final String eventFileData = new String(Files.readAllBytes(bodyTemplatePath));
        event.setBodyTemplate(eventFileData);
      }
    }

    // JsonUtils.marshal(Optional.of(simuraiConfig));
    exchange.getIn().setBody(events);
  }

  public void triggerEvent(final Exchange exchange) throws Exception {
    final Map<String, Object> headers = exchange.getIn().getHeaders();
    final String alias = (String) headers.get("alias"); //get request/query param by alias
    final Map<String, Object> dataMap = exchange.getIn().getBody(Map.class); // get requestBody
    log.debug(
            "Request received to trigger the event with name:{} having request body:{} ",
            alias,
            dataMap);

    final Optional<Endpoint> optEndpoint = simuraiConfig.getEndpoints().stream().findFirst();
    if (optEndpoint.isPresent()) {
      log.debug("Event found with name:{}", alias, dataMap);
      final Endpoint endpoint = optEndpoint.get();
      // simuraiConfig.getEventAlias(event.getAlias());
      final Path headerTemplatePath =
              Paths.get(simuraiConfig.getResourceDirectory(), alias, HEADER_FTL);
      final Path transformerTemplatePath =
              Paths.get(simuraiConfig.getResourceDirectory(), alias, TRANSFORMER_FTL);
      if (Files.exists(transformerTemplatePath)) {
        log.debug(
                "Event name:{} transformer template URI:{}",
                alias,
                transformerTemplatePath.toAbsolutePath().toString());
        final String headerData =
                domainToFtlTemplateTransformer.transform(
                        dataMap, FILE_COMPONENT + headerTemplatePath.toAbsolutePath().toString());
        final String tranformedData =
                domainToFtlTemplateTransformer.transform(
                        dataMap, FILE_COMPONENT + transformerTemplatePath.toAbsolutePath().toString());
        exchange.getIn().setBody(headerData);
        exchange.getIn().setBody(tranformedData);
        exchange.setProperty(TARGET_URI, endpoint.getUri());



      }
    }
  }
}
