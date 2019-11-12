package com.swetha.pricing.api;

import com.swetha.pricing.service.OandaDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Collection;

@RestController
public class DataController {

    private final OandaDataService oandaDataService;

    @Autowired
    public DataController(OandaDataService oandaDataService) {
        this.oandaDataService = oandaDataService;
    }

    @GetMapping("/data")
    public Collection<OandaDataService.Values> getData(@RequestParam(value = "date") String date) throws IOException {
        return oandaDataService.getData(date).values();
    }

    @GetMapping("/makeData")
    public String makeData() throws InterruptedException {
        oandaDataService.makeData();
        return "DONE";
    }
}
