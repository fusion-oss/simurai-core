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

import static com.scoperetail.simurai.core.common.constant.CamelComponentConstants.FILE_COMPONENT;
import static com.scoperetail.simurai.core.common.constant.ExchangeHeaderConstants.ALIAS;
import static com.scoperetail.simurai.core.common.constant.ExchangePropertyConstants.TARGET_URI;
import static com.scoperetail.simurai.core.common.constant.ResourceNameConstants.HEADER_TEMPLATE_NAME;
import static com.scoperetail.simurai.core.common.constant.ResourceNameConstants.TRANSFORMER_TEMPLATE_NAME;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.camel.Exchange;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import com.scoperetail.simurai.core.application.route.event.dto.EventDTO;
import com.scoperetail.simurai.core.application.service.transform.impl.DomainToFtlTemplateTransformer;
import com.scoperetail.simurai.core.common.util.JsonUtils;
import com.scoperetail.simurai.core.config.Endpoint;
import com.scoperetail.simurai.core.config.Event;
import com.scoperetail.simurai.core.config.SimuraiConfig;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EventBean {

  @Autowired private SimuraiConfig simuraiConfig;
  @Autowired private DomainToFtlTemplateTransformer domainToFtlTemplateTransformer;

  public void fetchEvents(final Exchange exchange) throws Exception {
    log.debug("Request received to fetch the events");
    final List<EventDTO> events = new ArrayList<>();
    for (final Event event : simuraiConfig.getEvents()) {
      events.add(buildEventDto(event));
    }
    exchange.getIn().setBody(events);
  }

  private EventDTO buildEventDto(final Event event) throws IOException {
    final Optional<Endpoint> optEndpoint = simuraiConfig.getEndpoint(event.getAlias());
    return EventDTO.builder()
        .name(event.getName())
        .alias(event.getAlias())
        .format(event.getFormat())
        .category(event.getCategory())
        .source(event.getSource())
        .headerTemplate(
            getTemplate(
                Paths.get(
                    simuraiConfig.getResourceDirectory(), event.getAlias(), HEADER_TEMPLATE_NAME)))
        .bodyTemplate(
            getTemplate(
                Paths.get(
                    simuraiConfig.getResourceDirectory(),
                    event.getAlias(),
                    TRANSFORMER_TEMPLATE_NAME)))
        .errorQueue(event.getErrorQueue())
		.usage(event.getUsage())
        .targetEndpoint(optEndpoint.orElse(null))
        .build();
  }

  private String getTemplate(final Path templatePath) throws IOException {
    String template = null;
    if (Files.exists(templatePath)) {
      template = new String(Files.readAllBytes(templatePath));
      template = template.replaceAll("(\\s)", "");
    }
    return template;
  }

  public void triggerEvent(final Exchange exchange) throws Exception {
    final Map<String, Object> headers = exchange.getIn().getHeaders();
    final String eventAlias = (String) headers.get(ALIAS);
    final Map<String, Object> dataMap = exchange.getIn().getBody(Map.class);
    log.debug(
        "Request received to trigger the event with eventAlias:{} having request body:{} ",
        eventAlias,
        dataMap);
    final Optional<Endpoint> optEndpoint = simuraiConfig.getEndpoint(eventAlias);
    if (optEndpoint.isPresent()) {
      log.debug("Event found with name:{}", eventAlias);
      final Endpoint endpoint = optEndpoint.get();

      final String headerData = getTransformedData(eventAlias, HEADER_TEMPLATE_NAME, dataMap);
      if (StringUtils.isNotBlank(headerData)) {
        final Map<String, Object> header =
            JsonUtils.unmarshal(Optional.of(headerData), Map.class.getCanonicalName());
        exchange.getIn().setHeaders(header);
      }

      final String bodyData = getTransformedData(eventAlias, TRANSFORMER_TEMPLATE_NAME, dataMap);
      if (StringUtils.isNotBlank(bodyData)) {
        exchange.getIn().setBody(bodyData);
      }
      exchange.setProperty(TARGET_URI, endpoint.getUri());
    }
  }

  private String getTransformedData(
      final String eventAlias, final String templateName, final Map<String, Object> dataMap)
      throws Exception {
    final Path templatePath =
        Paths.get(simuraiConfig.getResourceDirectory(), eventAlias, templateName);
    String transformedData = null;
    if (Files.exists(templatePath)) {
      transformedData =
          domainToFtlTemplateTransformer.transform(
              dataMap, FILE_COMPONENT + templatePath.toAbsolutePath().toString());
    }
    return transformedData;
  }
}
