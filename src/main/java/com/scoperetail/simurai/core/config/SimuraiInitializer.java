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

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.zeroturnaround.zip.ZipUtil;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SimuraiInitializer implements ApplicationListener<ContextRefreshedEvent> {

  private final SimuraiConfig simuraiConfig;
  private final ResourceLoader resourceLoader;
  private final String resourceDirectory;

  public SimuraiInitializer(
      final SimuraiConfig simuraiConfig,
      final ResourceLoader resourceLoader,
      @Value("${RESOURCE_DIRECTORY}") final String resourceDirectory) {
    this.simuraiConfig = simuraiConfig;
    this.resourceLoader = resourceLoader;
    this.resourceDirectory = resourceDirectory;
  }

  @Override
  public void onApplicationEvent(final ContextRefreshedEvent event) {
    try {
      simuraiConfig.setResourceDirectory(resourceDirectory);
      downloadResources();
    } catch (final Exception e) {
      log.error("Exception occured during initialization: {}", e);
      throw new RuntimeException(e);
    }
  }

  private void downloadResources() throws IOException {
    final String resourceURL = simuraiConfig.getResourceURL();
    if (StringUtils.isNotBlank(resourceURL)) {
      final Resource resource = resourceLoader.getResource(simuraiConfig.getResourceURL());
      log.info("Downloaded resources from URL: {}", resourceURL);
      final File resourceDir = getResourceDir();
      ZipUtil.unpack(resource.getInputStream(), resourceDir);
    } else {
      log.warn("Resource URL is not specified, falling back to local resource directory");
    }
  }

  private File getResourceDir() throws IOException {
    final File file = new File(Paths.get(resourceDirectory).toString());
    if (file.exists()) {
      FileUtils.cleanDirectory(file);
    } else {
      FileUtils.forceMkdir(file);
    }
    return file;
  }
}
