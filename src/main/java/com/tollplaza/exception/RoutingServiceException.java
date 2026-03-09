package com.tollplaza.exception;

public class RoutingServiceException extends RuntimeException {
    
    public RoutingServiceException(String message) {
        super(message);
    }
    
    public RoutingServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
