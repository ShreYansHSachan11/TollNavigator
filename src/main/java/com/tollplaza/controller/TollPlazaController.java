package com.tollplaza.controller;

import com.tollplaza.model.ErrorResponse;
import com.tollplaza.model.TollPlazaRequest;
import com.tollplaza.model.TollPlazaResponse;
import com.tollplaza.service.TollPlazaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for toll plaza discovery endpoints.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Toll Plaza", description = "API for discovering toll plazas between two Indian pincodes")
public class TollPlazaController {
    
    private static final Logger logger = LoggerFactory.getLogger(TollPlazaController.class);
    
    private final TollPlazaService tollPlazaService;
    
    public TollPlazaController(TollPlazaService tollPlazaService) {
        this.tollPlazaService = tollPlazaService;
    }
    
    /**
     * Finds toll plazas between source and destination pincodes.
     * 
     * @param request Request containing source and destination pincodes
     * @return ResponseEntity with TollPlazaResponse containing route and toll plazas
     */
    @PostMapping("/toll-plazas")
    @Operation(
        summary = "Find toll plazas between two pincodes",
        description = "Identifies all toll plazas located along the route between source and destination Indian pincodes. " +
                      "Returns route information and a list of toll plazas ordered by distance from source."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully found toll plazas",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TollPlazaResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request - validation failed (invalid pincode format or same source/destination)",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error - unexpected error occurred",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "503",
            description = "Service unavailable - external routing service is temporarily unavailable",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    public ResponseEntity<TollPlazaResponse> findTollPlazas(
            @Valid @RequestBody TollPlazaRequest request) {
        
        logger.info("Received request to find toll plazas: {}", request);
        
        TollPlazaResponse response = tollPlazaService.findTollPlazas(
                request.getSourcePincode(),
                request.getDestinationPincode()
        );
        
        logger.info("Successfully processed toll plaza request");
        
        return ResponseEntity.ok(response);
    }
}
