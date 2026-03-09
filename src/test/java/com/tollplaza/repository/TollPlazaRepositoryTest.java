package com.tollplaza.repository;

import com.tollplaza.exception.DataLoadException;
import com.tollplaza.model.TollPlaza;
import com.tollplaza.util.CsvDataLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TollPlazaRepositoryTest {
    
    @Mock
    private CsvDataLoader csvDataLoader;
    
    private TollPlazaRepository repository;
    
    @BeforeEach
    void setUp() {
        repository = new TollPlazaRepository(csvDataLoader);
    }
    
    @Test
    void testFindAll_ReturnsAllTollPlazas() {
        // Arrange
        List<TollPlaza> mockPlazas = Arrays.asList(
            new TollPlaza("Plaza 1", 28.4595, 77.0266),
            new TollPlaza("Plaza 2", 29.3909, 76.9635)
        );
        when(csvDataLoader.loadTollPlazas(anyString())).thenReturn(mockPlazas);
        
        // Act
        repository.loadFromCsv("test.csv");
        List<TollPlaza> result = repository.findAll();
        
        // Assert
        assertEquals(2, result.size());
        assertEquals("Plaza 1", result.get(0).getName());
        assertEquals("Plaza 2", result.get(1).getName());
    }
    
    @Test
    void testFindByName_ReturnsMatchingTollPlaza() {
        // Arrange
        List<TollPlaza> mockPlazas = Arrays.asList(
            new TollPlaza("Delhi-Gurgaon Toll Plaza", 28.4595, 77.0266),
            new TollPlaza("Panipat Toll Plaza", 29.3909, 76.9635)
        );
        when(csvDataLoader.loadTollPlazas(anyString())).thenReturn(mockPlazas);
        
        // Act
        repository.loadFromCsv("test.csv");
        Optional<TollPlaza> result = repository.findByName("Delhi-Gurgaon Toll Plaza");
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals("Delhi-Gurgaon Toll Plaza", result.get().getName());
    }
    
    @Test
    void testFindByName_CaseInsensitive() {
        // Arrange
        List<TollPlaza> mockPlazas = Arrays.asList(
            new TollPlaza("Delhi-Gurgaon Toll Plaza", 28.4595, 77.0266)
        );
        when(csvDataLoader.loadTollPlazas(anyString())).thenReturn(mockPlazas);
        
        // Act
        repository.loadFromCsv("test.csv");
        Optional<TollPlaza> result = repository.findByName("delhi-gurgaon toll plaza");
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals("Delhi-Gurgaon Toll Plaza", result.get().getName());
    }
    
    @Test
    void testFindByName_NotFound() {
        // Arrange
        List<TollPlaza> mockPlazas = Arrays.asList(
            new TollPlaza("Delhi-Gurgaon Toll Plaza", 28.4595, 77.0266)
        );
        when(csvDataLoader.loadTollPlazas(anyString())).thenReturn(mockPlazas);
        
        // Act
        repository.loadFromCsv("test.csv");
        Optional<TollPlaza> result = repository.findByName("Nonexistent Plaza");
        
        // Assert
        assertFalse(result.isPresent());
    }
    
    @Test
    void testLoadFromCsv_ThrowsDataLoadException_WhenCsvMissing() {
        // Arrange
        when(csvDataLoader.loadTollPlazas(anyString()))
            .thenThrow(new RuntimeException("CSV file not found"));
        
        // Act & Assert
        assertThrows(DataLoadException.class, () -> {
            repository.loadFromCsv("missing.csv");
        });
    }
    
    @Test
    void testLoadFromCsv_ClearsExistingData() {
        // Arrange
        List<TollPlaza> firstLoad = Arrays.asList(
            new TollPlaza("Plaza 1", 28.4595, 77.0266)
        );
        List<TollPlaza> secondLoad = Arrays.asList(
            new TollPlaza("Plaza 2", 29.3909, 76.9635),
            new TollPlaza("Plaza 3", 30.7333, 76.7794)
        );
        
        when(csvDataLoader.loadTollPlazas("first.csv")).thenReturn(firstLoad);
        when(csvDataLoader.loadTollPlazas("second.csv")).thenReturn(secondLoad);
        
        // Act
        repository.loadFromCsv("first.csv");
        assertEquals(1, repository.findAll().size());
        
        repository.loadFromCsv("second.csv");
        List<TollPlaza> result = repository.findAll();
        
        // Assert
        assertEquals(2, result.size());
        assertEquals("Plaza 2", result.get(0).getName());
        assertEquals("Plaza 3", result.get(1).getName());
    }
}
