package com.empresa.reporter;

import com.empresa.reporter.config.ReporterConfig;
import com.empresa.reporter.core.ReportContext;
import com.empresa.reporter.listener.TestNGReportListener;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

@Listeners(TestNGReportListener.class)
public class SampleSeleniumTest {

    private WebDriver driver;

    @BeforeClass
    public static void configureReporter() {
        ReportContext.setConfig(ReporterConfig.builder()
                .projectName("selenium-reporter Demo")
                .outputDir("./test-reports")
                .captureScreenshotOnFailure(true)
                .captureScreenshotOnSuccess(false)
                .openBrowserAfterReport(false)
                .build());
    }

    @BeforeMethod
    public void setUp() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage",
                "--window-size=1280,800");
        driver = new ChromeDriver(options);
        ReportContext.setDriver(driver);
    }

    @AfterMethod
    public void tearDown() {
        ReportContext.removeDriver();
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    public void testPageTitleContainsExpectedText() {
        driver.get("https://example.com");
        String title = driver.getTitle();
        Assert.assertTrue(title.contains("Example Domain"), "Title was: " + title);
    }

    @Test
    public void testHeadingIsVisible() {
        driver.get("https://example.com");
        String heading = driver.findElement(By.tagName("h1")).getText();
        Assert.assertEquals(heading, "Example Domain");
    }

    @Test
    public void testMoreInfoLinkExists() {
        driver.get("https://example.com");
        int links = driver.findElements(By.tagName("a")).size();
        Assert.assertTrue(links > 0, "Expected at least one link on the page");
    }

    // Intentionally fails to demonstrate screenshot capture and failure reporting
    @Test
    public void testIntentionalFailure_DemoScreenshot() {
        driver.get("https://example.com");
        Assert.fail("This test is intentionally failing to show the screenshot + stack trace in the report");
    }
}
