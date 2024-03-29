package com.swetha.pricing.service;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
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

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class OandaDataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OandaDataService.class);

    private static final ThreadLocal<WebDriver> webDriverThreadLocal = new ThreadLocal<>();
    private SeleniumWorker seleniumWorker;
    private AwsFileService awsFileService;

    private Map<String, Map<String, Values>> data;

    private SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");

    @Autowired
    public OandaDataService(SeleniumWorker seleniumWorker, AwsFileService awsFileService) {
        this.data = new LinkedHashMap<String, Map<String, Values>>() {
            @Override
            protected boolean removeEldestEntry(final Map.Entry eldest) {
                return size() > 10;
            }
        };
        this.seleniumWorker = seleniumWorker;
        this.awsFileService = awsFileService;
    }

    public void clearData(String date) {
        awsFileService.deleteFile(getFile(date));
        data.remove(date);
    }

    public Map<String, Values> getTodaysData() throws IOException {
        Date date = new Date();
        String strDate = formatter.format(date);

        return getData(strDate);
    }

    public Map<String, Values> getData(String date) throws IOException {
        Map<String, Values> valuesMap;
        if (!hasData(date)) {
            Map<String, Values> values = readFromFile(date);
            if (data.size() < 10) {
                data.put(date, values);
            }
            valuesMap = values;
        } else {
            valuesMap = data.get(date);
        }

        LOGGER.info("Data contains {} entries", valuesMap.values().size());
        return valuesMap;
    }

    @PostConstruct
    public void makeData() {
        Thread thread = new Thread(() -> {
            while (true) {
                Date date = new Date();
                String strDate = formatter.format(date);
                try {
                    if (!hasData(strDate)) {
                        LOGGER.info("Data not found for date={}", strDate);
                        initMap(strDate);
                        if (readFromFile(strDate).isEmpty()) {
                            LOGGER.info("Data not available on disk for date={}, will fetch from site", strDate);
                            fetchCurrencies(strDate);
                            writeToFile(strDate);
                        } else {
                            LOGGER.info("Data found in file for date={}", strDate);
                        }
                    } else {
                        LOGGER.info("Data already available for date={}, num entries={}", strDate, data.get(strDate).values().size());
                    }
                    Thread.sleep(TimeUnit.MINUTES.toMillis(15));
                } catch (IOException | InterruptedException ex) {
                    LOGGER.error("Failed to fetch prices for date={}", strDate, ex);
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private boolean hasData(String date) {
        return data.containsKey(date) && !data.get(date).isEmpty();
    }

    private void writeToFile(String date) {
        LOGGER.info("Started writing data to file, file={}", getFile(date));
        StringWriter stringWriter = new StringWriter();
        try (CSVWriter writer = new CSVWriter(stringWriter)) {
            writer.writeNext(new String[]{"Currency", "Currency Name", "Price 90 day average", "Price 180 day average", "Price today " + date});
            data.get(date).values().forEach(x -> {
                writer.writeNext(new String[]{x.currency, x.currencyName, x.price90Avg, x.price180Avg, x.priceCurrent});
            });

            awsFileService.uploadFile(stringWriter.toString(), getFile(date));
            stringWriter.close();
            LOGGER.info("Finished uploading file, file={}", getFile(date));
        } catch (Exception ex) {
            LOGGER.error("Writing to file failed, file={}", getFile(date), ex);
        }
    }

    private boolean fileExists(String date) {
        return awsFileService.fileExists(getFile(date));
    }

    private String getFile(String date) {
        return "exchange-rates-" + date + ".csv";
    }

    private Map<String, Values> readFromFile(String date) throws IOException {
        Map<String, Values> dateValues = new HashMap<>();
        if (fileExists(date)) {
            try (CSVReader reader = new CSVReader(new StringReader(awsFileService.downloadFile(getFile(date))))) {
                reader.readNext();

                for (String[] row : reader) {
                    Values values = new Values();
                    values.currency = row[0];
                    values.currencyName = row[1];
                    values.price90Avg = row[2];
                    values.price180Avg = row[3];
                    values.priceCurrent = row[4];
                    dateValues.put(values.currency, values);
                }
            }
        }
        return dateValues;
    }

    private void initMap(String date) {
        if (!data.containsKey(date)) {
            data.put(date, new HashMap<>());
        }
    }

    private void fetchCurrencies(String date) throws InterruptedException, IOException {
        LOGGER.info("Loading browser...");
        WebDriver webDriver = seleniumWorker.getDriver();
        try {
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

                data.get(date).put(currency, values);
            }
        } finally {
            try {
                webDriver.close();
                webDriver.quit();
            } catch (Exception ex) {
                LOGGER.error(ex.getMessage());
            }
        }
    }

    private boolean hasClass(WebElement element, String className) {
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
