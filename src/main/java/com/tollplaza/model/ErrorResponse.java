package com.tollplaza.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Error response returned when request processing fails")
public class ErrorResponse {
    
    @Schema(description = "Error message describing what went wrong", example = "Invalid source or destination pincode")
    private String error;
    
    @Schema(description = "Timestamp when the error occurred (ISO 8601 format)", example = "2024-01-15T10:30:00Z")
    private String timestamp;
    
    @Schema(description = "Request path that caused the error", example = "/api/v1/toll-plazas")
    private String path;
    
    @Schema(description = "HTTP status code", example = "400")
    private int status;

    public ErrorResponse() {
        this.timestamp = Instant.now().toString();
    }

    public ErrorResponse(String error, String path, int status) {
        this.error = error;
        this.timestamp = Instant.now().toString();
        this.path = path;
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "ErrorResponse{" +
                "error='" + error + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", path='" + path + '\'' +
                ", status=" + status +
                '}';
    }
}
