package com.scoperetail.simurai.core.config;

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

import static com.scoperetail.simurai.core.common.constant.SimuraiConstants.EVENT_ALIAS;
import static com.scoperetail.simurai.core.common.constant.SimuraiConstants.TARGET_URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "simurai")
@Data
public class SimuraiConfig {
  private String resourceDirectory;
  private String resourceURL;
  private List<Event> events;
  private final List<AMQPBroker> amqpBrokers = new ArrayList<>(1);
  private final List<Endpoint> endpoints = new ArrayList<>(1);
  private final List<String> categories = new ArrayList<>(1);
  private final List<String> inboundEndpoints = new ArrayList<>(1);
  private final List<EventEndpointMapping> eventEndpointMappings = new ArrayList<>(1);

  public Optional<Event> getEventByName(final String eventName) {
    return events.stream().filter(e -> e.getName().equals(eventName)).findFirst();
  }

  public Optional<Endpoint> getEndpoint(final String alias) {

    final Optional<Optional<Map<String, Object>>> eventMapping =
        eventEndpointMappings
            .stream()
            .map(
                eem ->
                    eem.getEvents()
                        .stream()
                        .filter(event -> event.get(EVENT_ALIAS).equals(alias))
                        .findFirst())
            .findFirst();
    Optional<Endpoint> optEndpoint = Optional.ofNullable(null);
    if (eventMapping.isPresent()) {
      final Optional<Map<String, Object>> optEventMap = eventMapping.get();
      if (optEventMap.isPresent()) {
        final Object targetUrl = optEventMap.get().get(TARGET_URL);
        optEndpoint =
            getEndpoints()
                .stream()
                .filter(e -> e.getName().equals(targetUrl.toString()))
                .findFirst();
      }
    }
    return optEndpoint;
  }
}
