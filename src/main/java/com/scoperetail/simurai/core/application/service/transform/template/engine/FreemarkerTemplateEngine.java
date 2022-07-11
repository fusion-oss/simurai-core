package com.scoperetail.simurai.core.application.service.transform.template.engine;

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

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.apache.camel.CamelContext;
import org.apache.camel.component.freemarker.FreemarkerComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import freemarker.template.Configuration;
import freemarker.template.Template;

@Component
public class FreemarkerTemplateEngine implements TemplateEngine {
  public static final String FTL_EXTENSION = ".ftl";
  private FreemarkerComponent freemarkerComponent;
  @Autowired private CamelContext camelContext;

  @PostConstruct
  private void init() {
    freemarkerComponent = camelContext.getComponent("freemarker", FreemarkerComponent.class);
    final Configuration configuration = freemarkerComponent.getConfiguration();
    configuration.setNumberFormat("computer");
    freemarkerComponent.setConfiguration(configuration);
  }

  @Override
  public String generateTextFromTemplate(
      final Map<String, Object> params, final String templatePath) {
    try {
      final Template template =
          freemarkerComponent.getConfiguration().getTemplate(StringUtils.cleanPath(templatePath));
      final HashMap<String, Object> paramsMap = new HashMap<>(params);
      final StringWriter writer = new StringWriter();
      template.process(paramsMap, writer);
      final String text = writer.toString();
      return text;
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }
}
