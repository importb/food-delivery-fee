package com.delivery.fee.exception;

/**
 * Custom exception to throw when the weather is too bad for the selected vehicle type.
 */
public class VehicleForbiddenException extends RuntimeException {
    public VehicleForbiddenException(String message) {
        super(message);
    }
}