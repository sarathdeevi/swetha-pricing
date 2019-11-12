package com.swetha.pricing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@SpringBootApplication
public class Application {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Application.class, args);

        openHomePage();
    }

    private static void openHomePage() {
        try {
            String port = System.getProperty("server.port");
            System.out.println("Port=" + port);

            URI homepage = new URI("http://localhost:/" + port);
            Desktop.getDesktop().browse(homepage);
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
    }
}
