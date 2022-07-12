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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.scoperetail.simurai.core.config.camel.AMQPBroker;
import com.scoperetail.simurai.core.config.camel.Endpoint;
import com.scoperetail.simurai.core.config.camel.Event;
import com.scoperetail.simurai.core.config.camel.EventEndpointMapping;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Configuration
@ConfigurationProperties(prefix = "simurai")
@Getter
@Setter
@ToString
public class SimuraiConfig {
  private String resourceDirectory;
  private String resourceURL;
  private List<Event> events;
  private final List<AMQPBroker> amqpBrokers = new ArrayList<>(1);
  private final List<Endpoint> endpoints = new ArrayList<>(1);
  private final List<String> categories = new ArrayList<>(1);
  private final List<EventEndpointMapping> endpointMapping = new ArrayList<>(1);

  public Optional<Endpoint> getEndpointbyname(final String endpointName)
  {
    // return getEndpointbyname(endpointName);
    return getEndpoints().stream().filter(e -> e.getName().equals(endpointName)).findFirst();
  }



  public Optional<Event> getEventByName(final String eventName) {
    return getEvents().stream().filter(e -> e.getName().equals(eventName)).findFirst();
  }
}
