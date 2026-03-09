package com.tollplaza.repository;

import com.tollplaza.exception.DataLoadException;
import com.tollplaza.model.TollPlaza;
import com.tollplaza.util.CsvDataLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class TollPlazaRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(TollPlazaRepository.class);
    
    private final CsvDataLoader csvDataLoader;
    private final List<TollPlaza> tollPlazas;
    
    @Value("${tollplaza.csv.path:classpath:data/toll-plazas.csv}")
    private String csvFilePath;
    
    public TollPlazaRepository(CsvDataLoader csvDataLoader) {
        this.csvDataLoader = csvDataLoader;
        this.tollPlazas = new ArrayList<>();
    }
    
    @PostConstruct
    public void init() {
        loadFromCsv(csvFilePath);
    }
    
    public List<TollPlaza> findAll() {
        return new ArrayList<>(tollPlazas);
    }
    
    public Optional<TollPlaza> findByName(String name) {
        return tollPlazas.stream()
                .filter(tp -> tp.getName().equalsIgnoreCase(name))
                .findFirst();
    }
    
    public void loadFromCsv(String csvFilePath) {
        try {
            logger.info("Loading toll plaza data from: {}", csvFilePath);
            List<TollPlaza> loadedPlazas = csvDataLoader.loadTollPlazas(csvFilePath);
            
            if (loadedPlazas.isEmpty()) {
                logger.warn("No toll plazas loaded from CSV file: {}", csvFilePath);
            }
            
            tollPlazas.clear();
            tollPlazas.addAll(loadedPlazas);
            
            logger.info("Successfully loaded {} toll plazas into repository", tollPlazas.size());
            
        } catch (Exception e) {
            logger.error("Failed to load toll plaza data from CSV: {}", csvFilePath, e);
            throw new DataLoadException("Failed to load toll plaza data from CSV: " + csvFilePath, e);
        }
    }
}
