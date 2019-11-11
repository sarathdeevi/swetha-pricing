package com.swetha.pricing.api;

import com.swetha.pricing.service.OandaDataService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class DataController {

    private final OandaDataService oandaDataService;

    public DataController(OandaDataService oandaDataService) {
        this.oandaDataService = oandaDataService;
    }

    @GetMapping("/data")
    public Map<String, OandaDataService.Values> uploadFile() {
        return oandaDataService.getData();
    }
}
