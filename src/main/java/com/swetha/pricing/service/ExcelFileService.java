package com.swetha.pricing.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;

@Service
public class ExcelFileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExcelFileService.class);

    private final OandaDataService oandaDataService;

    @Autowired
    public ExcelFileService(OandaDataService oandaDataService) {
        this.oandaDataService = oandaDataService;
    }

    public File updateFile(MultipartFile multipartFile) throws IOException {
        Map<String, OandaDataService.Values> todaysValues = oandaDataService.getTodaysData();

        File file = getFile(multipartFile.getOriginalFilename());
        try (InputStream is = multipartFile.getInputStream()) {
            XSSFWorkbook workbook = new XSSFWorkbook(is);

            XSSFSheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();
            boolean currencies = false;

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();

                Iterator<Cell> cellIterator = row.cellIterator();
                if (cellIterator.hasNext()) {
                    if (currencies) {
                        String currency = cellIterator.next().getStringCellValue();
                        cellIterator.next();

                        if (StringUtils.length(StringUtils.trim(currency)) == 3) {
                            OandaDataService.Values values = todaysValues.get(currency);
                            if (values != null) {
                                cellIterator.next().setCellValue(Float.parseFloat(values.price90Avg));
                                cellIterator.next().setCellValue(Float.parseFloat(values.price180Avg));
                                cellIterator.next().setCellValue(Float.parseFloat(values.priceCurrent));
                            } else {
                                LOGGER.warn("Values not found for currency={}", currency);
                            }
                        }
                    } else if ("INR".equalsIgnoreCase(cellIterator.next().getStringCellValue())) {
                        currencies = true;
                    }
                }
            }
            FileOutputStream fos = new FileOutputStream(file);
            workbook.write(fos);
            fos.close();
        }
        return file;
    }

    private File getFile(String name) {
        File outputDirectory = new File(System.getProperty("user.home"), "prices/output");
        outputDirectory.mkdirs();
        return new File(outputDirectory, name);
    }

    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = getFile(fileName).toPath();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("File not found " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("File not found " + fileName, ex);
        }
    }
}
