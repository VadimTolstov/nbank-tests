package jupiter.extension;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import com.codeborne.selenide.logevents.SelenideLogger;
import io.qameta.allure.Allure;
import io.qameta.allure.selenide.AllureSelenide;
import org.junit.jupiter.api.extension.*;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.io.ByteArrayInputStream;

public class BrowserExtension implements
    BeforeEachCallback,
    AfterEachCallback,
    TestExecutionExceptionHandler,
    LifecycleMethodExecutionExceptionHandler {

  static {
    String browser = System.getenv("SELENOID_BROWSER");
    if (browser == null || browser.isEmpty() || "chrome".equals(browser)) {
      browser = "chrome";
    }

    Configuration.browser = browser;
    Configuration.timeout = 8000;
    Configuration.pageLoadStrategy = "eager";

    if ("firefox".equals(browser)) {
      Configuration.browserVersion = "125.0";
      FirefoxOptions firefoxOptions = new FirefoxOptions();
      firefoxOptions.addPreference("intl.accept_languages", "ru,ru-RU");
      firefoxOptions.addArguments("--lang=ru");
      Configuration.browserCapabilities = firefoxOptions;
    }

    if ("docker".equals(System.getProperty("test.env"))) {
      Configuration.remote = "http://selenoid:4444/wd/hub";
      if ("chrome".equals(browser)) {
        Configuration.browserVersion = "128.0";
        Configuration.browserCapabilities = new ChromeOptions()
            .addArguments(
                "--no-sandbox",
                "--lang=ru",
                "--accept-lang=ru,ru-RU"
            );
      }
    }
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    if (WebDriverRunner.hasWebDriverStarted()) {
      Selenide.closeWebDriver();
    }
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    SelenideLogger.addListener("Allure-selenide", new AllureSelenide()
        .savePageSource(false)
        .screenshots(false)
    );
    // Добавляем информацию о браузере в Allure
    Allure.addAttachment("Browser", "text/plain", Configuration.browser + " " + Configuration.browserVersion);
  }

  @Override
  public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
    doScreenshot();
    throw throwable;
  }

  @Override
  public void handleBeforeEachMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
    doScreenshot();
    throw throwable;
  }

  @Override
  public void handleAfterEachMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
    doScreenshot();
    throw throwable;
  }

  private static void doScreenshot() {
    if (WebDriverRunner.hasWebDriverStarted()) {
      Allure.addAttachment(
          "Screen on fail",
          new ByteArrayInputStream(
              ((TakesScreenshot) WebDriverRunner.getWebDriver()).getScreenshotAs(OutputType.BYTES)
          )
      );
    }
  }
}
