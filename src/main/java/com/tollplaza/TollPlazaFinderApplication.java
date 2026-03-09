package com.tollplaza;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TollPlazaFinderApplication {

    private static final Logger logger = LoggerFactory.getLogger(TollPlazaFinderApplication.class);

    public static void main(String[] args) {
        logger.info("Starting Toll Plaza Finder Application");
        SpringApplication.run(TollPlazaFinderApplication.class, args);
        logger.info("Toll Plaza Finder Application started successfully");
    }
}
