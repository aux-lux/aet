/**
 * AET
 *
 * Copyright (C) 2013 Cognifide Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.cognifide.aet.worker.drivers;

import com.cognifide.aet.job.api.collector.WebCommunicationWrapper;
import com.cognifide.aet.job.api.exceptions.ProxyException;
import com.cognifide.aet.proxy.ProxyServerProvider;
import com.cognifide.aet.worker.api.WebDriverFactory;
import com.cognifide.aet.worker.exceptions.WorkerException;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import java.util.Map;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author lukasz.wieczorek
 */
@Service(WebDriverProvider.class)
@Component(label = "AET WebDriver Provider", description = "AET WebDriver Provider", immediate = true, metatype = true)
@Properties({@Property(name = Constants.SERVICE_VENDOR, value = "Cognifide Ltd")})
public class WebDriverProvider {

  private static final Logger LOG = LoggerFactory.getLogger(WebDriverProvider.class);

  private static final String DEFAULT_WEB_DRIVER_NAME = "defaultWebDriverName";

  @Property(name = DEFAULT_WEB_DRIVER_NAME, label = "Default Web Driver name", value = "ff")
  private String defaultWebDriverName;

  @Reference(referenceInterface = WebDriverFactory.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, bind = "bindWebDriverFactory", unbind = "unbindWebDriverFactory")
  private final Map<String, WebDriverFactory> collectorFactories = Maps.newConcurrentMap();

  @Reference
  private ProxyServerProvider proxyServerProvider;

  @Activate
  void activate(Map<String, String> properties) {
    defaultWebDriverName = properties.get(DEFAULT_WEB_DRIVER_NAME);
  }

  public WebCommunicationWrapper createWebDriverWithProxy(String preferredWebDriver, String proxyName)
      throws WorkerException {
    WebDriverFactory webDriverFactory = findWebDriverFactory(preferredWebDriver);
    try {
      return webDriverFactory.createWebDriver(proxyServerProvider.createProxy(proxyName));
    } catch (ProxyException e) {
      throw new WorkerException(String.format("Failed to connect: %s", e.getMessage()), e);
    }
  }

  public WebCommunicationWrapper createWebDriver(String preferredWebDriver) throws WorkerException {
    WebDriverFactory webDriverFactory = findWebDriverFactory(preferredWebDriver);
    return webDriverFactory.createWebDriver();
  }

  protected void bindWebDriverFactory(WebDriverFactory webDriverFactory) {
    this.collectorFactories.put(webDriverFactory.getName(), webDriverFactory);
  }

  protected void unbindWebDriverFactory(WebDriverFactory webDriverFactory) {
    this.collectorFactories.remove(webDriverFactory.getName());
  }


  /**
   * Finds web driver factory for given ID or default one if the argument is null.
   *
   * @param preferredWebDriver web driver ID specified in suite XML.
   * @return web driver factory for given ID or default one.
   * @throws WorkerException if there is no factory registered for given id.
   */
  private WebDriverFactory findWebDriverFactory(String preferredWebDriver) throws WorkerException {
    final WebDriverFactory webDriverFactory;
    String id = preferredWebDriver == null ? defaultWebDriverName : preferredWebDriver;
    webDriverFactory = collectorFactories.get(id);
    if (webDriverFactory == null) {
      String webDrivers = Joiner.on(", ").join(collectorFactories.keySet());
      String errorMessage = String.format("Undefined WebDriver: '%s'. Available: %s", id, webDrivers);
      throw new WorkerException(errorMessage);
    }
    return webDriverFactory;
  }
}
