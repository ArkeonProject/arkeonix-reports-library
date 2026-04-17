package com.empresa.reporter.core;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScreenshotCapture {

    private static final Logger log = LoggerFactory.getLogger(ScreenshotCapture.class);

    private ScreenshotCapture() {}

    public static String capture(WebDriver driver) {
        if (driver == null) return null;
        try {
            if (driver instanceof TakesScreenshot) {
                return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
            }
            log.warn("WebDriver does not support TakesScreenshot");
        } catch (Exception e) {
            log.warn("Screenshot capture failed: {}", e.getMessage());
        }
        return null;
    }
}
