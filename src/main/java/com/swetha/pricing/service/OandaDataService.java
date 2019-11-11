package com.swetha.pricing.service;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OandaDataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OandaDataService.class);

    private static final ThreadLocal<WebDriver> webDriverThreadLocal = new ThreadLocal<>();
    private SeleniumWorker seleniumWorker;

    private Map<String, Values> data;

    @Autowired
    public OandaDataService(SeleniumWorker seleniumWorker) {
        this.seleniumWorker = seleniumWorker;
    }

    public Map<String, Values> getData() {
        return data;
    }

    public void makeData() throws InterruptedException, IOException {
        if (data == null) {
            data = fetchCurrencies();
        }
    }

    private Map<String, Values> fetchCurrencies() throws InterruptedException, IOException {
        LOGGER.info("Loading browser...");
        WebDriver webDriver = seleniumWorker.getDriver();
        webDriverThreadLocal.set(webDriver);
        webDriver.get("https://www1.oanda.com/fx-for-business/historical-rates");

        WebElement select = findElement(By.cssSelector("div.container.want .currency-container .want-select .select-container"));
        select.click();
        select.findElement(By.cssSelector(".currencyPicker .search")).sendKeys("INR");
        Thread.sleep(200);
        select.findElement(By.cssSelector(".currencyPicker .search")).sendKeys(Keys.DOWN);
        select.findElement(By.cssSelector(".currencyPicker .search")).sendKeys(Keys.ENTER);

        select = findElement(By.cssSelector("div.container.have .have-select .select-container"));
        select.click();
        Thread.sleep(200);
        List<WebElement> currencyList = select.findElement(By.cssSelector(".currencyPicker")).findElement(By.cssSelector("ul.list")).findElements(By.tagName("li"));
        Map<String, Values> currencies = new LinkedHashMap<>();

        LOGGER.info("Evaluating currencies...");
        for (WebElement element : currencyList) {
            if (!hasClass(element, "groupLabel")) {
                Values values = new Values();
                values.currency = element.findElement(By.className("value")).getText();
                values.currencyName = element.findElement(By.className("display")).getText();

                currencies.put(values.currency, values);
            }
        }
        select.click();

        for (String currency : currencies.keySet()) {
            LOGGER.info("Getting values for currency : {}", currency);
            Thread.sleep(200);
            select = findElement(By.cssSelector("div.container.have .have-select .select-container"));
            select.click();
            select.findElement(By.cssSelector(".currencyPicker .search")).sendKeys(currency);
            Thread.sleep(200);
            List<WebElement> currList = select.findElement(By.cssSelector(".currencyPicker")).findElement(By.cssSelector("ul.list")).findElements(By.tagName("li"));
            for (WebElement element : currList) {
                if (element.findElement(By.className("value")).getText().equalsIgnoreCase(currency)) {
                    element.click();
                    break;
                }
            }

            findElement(By.cssSelector("div.button.table")).click();
            findElement(By.name("range")).click();
            findElement(By.cssSelector("div.tableCell.ranges")).click();
            findElement(By.cssSelector("div.ranges .dropdown .table.item[data-days='90']")).click();
            findElement(By.cssSelector("button.apply")).click();

            Thread.sleep(400);
            waitForDisappear(By.id("widget-loader"));
            Thread.sleep(200);

            List<WebElement> rows = findElement(By.cssSelector("div#ht2 table tbody")).findElements(By.tagName("tr"));

            Values values = currencies.get(currency);

            values.price90Avg = rows.get(0).findElements(By.tagName("td")).get(1).getText();
            values.priceCurrent = rows.get(3).findElements(By.tagName("td")).get(1).getText();

            findElement(By.name("range")).click();
            findElement(By.cssSelector("div.tableCell.ranges")).click();
            findElement(By.cssSelector("div.ranges .dropdown .table.item[data-days='180']")).click();
            findElement(By.cssSelector("button.apply")).click();

            Thread.sleep(400);
            waitForDisappear(By.id("widget-loader"));
            Thread.sleep(200);

            rows = findElement(By.cssSelector("div#ht2 table tbody")).findElements(By.tagName("tr"));
            values.price180Avg = rows.get(0).findElements(By.tagName("td")).get(1).getText();

            LOGGER.info("Values for currency={}, are={}", currency, values);
        }

        return currencies;
    }

    public boolean hasClass(WebElement element, String className) {
        String classes = element.getAttribute("class");
        for (String c : classes.split(" ")) {
            if (c.equals(className)) {
                return true;
            }
        }

        return false;
    }

    private WebElement findElement(By by) {
        return (new WebDriverWait(webDriverThreadLocal.get(), 10))
                .until(ExpectedConditions.elementToBeClickable(by));
    }

    private void waitForDisappear(By by) {
        new WebDriverWait(webDriverThreadLocal.get(), 10).until(ExpectedConditions.invisibilityOfElementLocated(by));
    }

    public static final class Values {
        String currency;
        String currencyName;
        String price90Avg;
        String price180Avg;
        String priceCurrent;

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.SIMPLE_STYLE);
        }

        public String getCurrency() {
            return currency;
        }

        public String getCurrencyName() {
            return currencyName;
        }

        public String getPrice90Avg() {
            return price90Avg;
        }

        public String getPrice180Avg() {
            return price180Avg;
        }

        public String getPriceCurrent() {
            return priceCurrent;
        }
    }
}
