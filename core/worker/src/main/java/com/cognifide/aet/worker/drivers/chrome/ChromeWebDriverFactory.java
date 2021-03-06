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
package com.cognifide.aet.worker.drivers.chrome;

import static com.cognifide.aet.worker.drivers.WebDriverHelper.DEFAULT_SELENIUM_GRID_URL;
import static com.cognifide.aet.worker.drivers.WebDriverHelper.NAME;
import static com.cognifide.aet.worker.drivers.WebDriverHelper.NAME_DESC;
import static com.cognifide.aet.worker.drivers.WebDriverHelper.NAME_LABEL;
import static com.cognifide.aet.worker.drivers.WebDriverHelper.SELENIUM_GRID_URL;
import static com.cognifide.aet.worker.drivers.WebDriverHelper.SELENIUM_GRID_URL_LABEL;
import static com.cognifide.aet.worker.drivers.WebDriverHelper.getProp;
import static com.cognifide.aet.worker.drivers.WebDriverHelper.setupProxy;

import com.cognifide.aet.job.api.collector.HttpRequestExecutorFactory;
import com.cognifide.aet.job.api.collector.ProxyServerWrapper;
import com.cognifide.aet.job.api.collector.WebCommunicationWrapper;
import com.cognifide.aet.worker.api.WebDriverFactory;
import com.cognifide.aet.worker.exceptions.WorkerException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.osgi.framework.Constants;

@Service
@Component(
    policy = ConfigurationPolicy.REQUIRE,
    description = "AET Chrome WebDriver Factory",
    label = "AET Chrome WebDriver Factory",
    metatype = true)
@Properties({@Property(name = Constants.SERVICE_VENDOR, value = "Cognifide Ltd")})
public class ChromeWebDriverFactory implements WebDriverFactory {

  private static final String DEFAULT_BROWSER_NAME = "chrome";

  @Reference
  private HttpRequestExecutorFactory requestExecutorFactory;

  @Property(name = NAME,
      label = NAME_LABEL,
      description = NAME_DESC,
      value = DEFAULT_BROWSER_NAME)
  private String name;

  @Property(name = SELENIUM_GRID_URL,
      label = SELENIUM_GRID_URL_LABEL,
      description = "Url to selenium grid hub. When null local Chrome driver will be used. Local Chrome driver does not work on Linux",
      value = DEFAULT_SELENIUM_GRID_URL)
  private String seleniumGridUrl;

  @Activate
  public void activate(Map<String, String> properties) {
    this.name = getProp(properties, NAME, DEFAULT_BROWSER_NAME);
    this.seleniumGridUrl = getProp(properties, SELENIUM_GRID_URL, DEFAULT_SELENIUM_GRID_URL);
  }

  @Override
  public WebCommunicationWrapper createWebDriver() throws WorkerException {
    final DesiredCapabilities capabilities = setupCapabilities();

    return createWebDriver(capabilities, null);
  }

  @Override
  public WebCommunicationWrapper createWebDriver(ProxyServerWrapper proxyServer)
      throws WorkerException {
    final Proxy proxy = setupProxy(proxyServer);
    final DesiredCapabilities capabilities = setupCapabilities();
    capabilities.setCapability(CapabilityType.PROXY, proxy);

    return createWebDriver(capabilities, proxyServer);
  }

  @Override
  public String getName() {
    return name;
  }

  private WebCommunicationWrapper createWebDriver(DesiredCapabilities capabilities,
      ProxyServerWrapper proxyServer)
      throws WorkerException {
    try {
      return new ChromeCommunicationWrapperImpl(getChromeDriver(capabilities), proxyServer,
          requestExecutorFactory.createInstance());
    } catch (MalformedURLException e) {
      throw new WorkerException(e.getMessage(), e);
    }
  }

  private WebDriver getChromeDriver(DesiredCapabilities capabilities)
      throws MalformedURLException {
    WebDriver driver = StringUtils.isNotBlank(seleniumGridUrl) ? new RemoteWebDriver(
        new URL(seleniumGridUrl), capabilities) : new ChromeDriver(capabilities);
    driver.manage().timeouts().pageLoadTimeout(5L, TimeUnit.MINUTES);
    return driver;
  }

  private DesiredCapabilities setupCapabilities() {
    final DesiredCapabilities capabilities = DesiredCapabilities.chrome();
    capabilities.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);

    final LoggingPreferences loggingPreferences = new LoggingPreferences();
    loggingPreferences.enable(LogType.BROWSER, Level.ALL);
    capabilities.setCapability(CapabilityType.LOGGING_PREFS, loggingPreferences);

    ChromeOptions options = new ChromeOptions();
    options.addArguments("--disable-plugins");

    capabilities.setCapability(ChromeOptions.CAPABILITY, options);

    return capabilities;
  }
}
