package com.swetha.pricing.service;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
public class SeleniumWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeleniumWorker.class);

    private static String OS = System.getProperty("os.name").toLowerCase();

    private final AwsFileService awsFileService;

    @Autowired
    public SeleniumWorker(AwsFileService awsFileService) {
        this.awsFileService = awsFileService;
    }

    private static boolean isWindows() {
        return (OS.contains("win"));
    }

    private static boolean isMac() {
        return (OS.contains("mac"));
    }

    private static boolean isUnix() {
        return (OS.contains("nix") || OS.contains("nux") || OS.indexOf("aix") > 0);
    }

    private static boolean isSolaris() {
        return (OS.contains("sunos"));
    }

    private String getFileName() {
        String path = "";
        if (isWindows()) {
            path = "chromedriver.exe";
        } else if (isMac()) {
            path = "chromedriver-mac";
        } else if (isUnix()) {
            path = "chromedriver-linux";
        } else if (isSolaris()) {
            System.out.println("This is Solaris");
        } else {
            System.out.println("Your OS is not support!!");
        }
        LOGGER.info("Driver class path resource={}", path);
        return path;
    }

    public WebDriver getDriver() throws IOException {
        LOGGER.info("Fetching driver...");
        if (!isWebDriverAvailable()) {
            LOGGER.info("Driver is not available, setting up driver...");
            setUpWebDriver();
        }
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        return new ChromeDriver(options);
    }

    private boolean isWebDriverAvailable() {
        File copy = new File(System.getProperty("user.home"), getFileName());
        setDriverPath(copy);
        return copy.exists();
    }

    private void setUpWebDriver() throws IOException {
        File copy = new File(System.getProperty("user.home"), getFileName());

        FileUtils.copyInputStreamToFile(awsFileService.getFileAsStream(getFileName()), copy);
        if (isMac() || isUnix()) {
            Runtime.getRuntime().exec("chmod 777 " + copy.getAbsolutePath());
        }
        setDriverPath(copy);
    }

    private void setDriverPath(File file) {
        LOGGER.info("Setting driver path={}", file.getAbsolutePath());
        System.setProperty("webdriver.chrome.driver", file.getAbsolutePath());
    }
}
