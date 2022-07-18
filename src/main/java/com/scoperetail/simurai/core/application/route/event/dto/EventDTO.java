package com.scoperetail.simurai.core.application.route.event.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EventDTO {
  private String name;
  private String alias;
  private String format;
  private String category;
  private String source;
  private String headerTemplate;
  private String bodyTemplate;
}
