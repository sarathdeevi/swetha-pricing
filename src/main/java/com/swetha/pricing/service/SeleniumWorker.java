package com.swetha.pricing.service;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
public class SeleniumWorker {

    private static String OS = System.getProperty("os.name").toLowerCase();

    private final ResourceLoader resourceLoader;

    @Autowired
    public SeleniumWorker(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
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

    public WebDriver getDriver() throws IOException {
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
        System.setProperty("webdriver.chrome.driver", copyAndGetLocation(path));
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        return new ChromeDriver(options);
    }

    private String copyAndGetLocation(String file) throws IOException {
        Resource resource = resourceLoader.getResource("classpath:" + file);
        File copy = new File(System.getProperty("user.home"), resource.getFilename());

        FileUtils.copyInputStreamToFile(resource.getInputStream(), copy);
        if (isMac() || isUnix()) {
            Runtime.getRuntime().exec("chmod 777 " + copy.getAbsolutePath());
        }
        return copy.getAbsolutePath();
    }
}
