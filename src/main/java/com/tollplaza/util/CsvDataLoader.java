package com.tollplaza.util;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.tollplaza.model.TollPlaza;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Component
public class CsvDataLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(CsvDataLoader.class);
    
    public List<TollPlaza> loadTollPlazas(String csvFilePath) {
        List<TollPlaza> tollPlazas = new ArrayList<>();
        
        try {
            List<String[]> records = readCsvFile(csvFilePath);
            
            // Skip header row if present
            boolean isFirstRow = true;
            for (String[] record : records) {
                if (isFirstRow) {
                    isFirstRow = false;
                    // Check if first row is header
                    if (isHeaderRow(record)) {
                        continue;
                    }
                }
                
                TollPlaza tollPlaza = parseCsvRecord(record);
                if (tollPlaza != null) {
                    tollPlazas.add(tollPlaza);
                }
            }
            
            logger.info("Successfully loaded {} toll plazas from {}", tollPlazas.size(), csvFilePath);
            
        } catch (IOException e) {
            logger.error("Failed to read CSV file: {}", csvFilePath, e);
            throw new RuntimeException("Failed to load toll plaza data from CSV", e);
        } catch (CsvException e) {
            logger.error("Failed to parse CSV file: {}", csvFilePath, e);
            throw new RuntimeException("Failed to parse toll plaza CSV data", e);
        }
        
        return tollPlazas;
    }
    
    private List<String[]> readCsvFile(String csvFilePath) throws IOException, CsvException {
        // Try to load from classpath first
        if (csvFilePath.startsWith("classpath:")) {
            String resourcePath = csvFilePath.substring("classpath:".length());
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
            
            if (inputStream == null) {
                throw new IOException("CSV file not found in classpath: " + resourcePath);
            }
            
            try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream))) {
                return reader.readAll();
            }
        } else {
            // Load from file system
            try (CSVReader reader = new CSVReader(new FileReader(csvFilePath))) {
                return reader.readAll();
            }
        }
    }
    
    private boolean isHeaderRow(String[] record) {
        // Check if first column looks like a header (contains "longitude" or "name")
        if (record.length > 0) {
            String firstColumn = record[0].toLowerCase().trim();
            return firstColumn.equals("name") || 
                   firstColumn.equals("toll_plaza") || 
                   firstColumn.equals("plaza_name") ||
                   firstColumn.equals("longitude");
        }
        // Also check if third column contains "toll_name" header
        if (record.length > 2) {
            String thirdColumn = record[2].toLowerCase().trim();
            if (thirdColumn.equals("toll_name")) {
                return true;
            }
        }
        return false;
    }
    
    private TollPlaza parseCsvRecord(String[] fields) {
        try {
            // Expected format: longitude, latitude, toll_name, geo_state
            if (fields.length < 3) {
                logger.warn("Skipping malformed CSV record: insufficient fields (expected at least 3, got {})", fields.length);
                return null;
            }
            
            double longitude = Double.parseDouble(fields[0].trim());
            double latitude = Double.parseDouble(fields[1].trim());
            String name = fields[2].trim();
            
            // Validate coordinates
            if (latitude < -90 || latitude > 90) {
                logger.warn("Skipping record with invalid latitude: {} (must be between -90 and 90)", latitude);
                return null;
            }
            
            if (longitude < -180 || longitude > 180) {
                logger.warn("Skipping record with invalid longitude: {} (must be between -180 and 180)", longitude);
                return null;
            }
            
            if (name.isEmpty()) {
                logger.warn("Skipping record with empty name");
                return null;
            }
            
            return new TollPlaza(name, latitude, longitude);
            
        } catch (NumberFormatException e) {
            logger.warn("Skipping malformed CSV record: invalid number format - {}", e.getMessage());
            return null;
        } catch (Exception e) {
            logger.warn("Skipping malformed CSV record: {}", e.getMessage());
            return null;
        }
    }
}
