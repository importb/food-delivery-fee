package com.delivery.fee.model;

/**
 * Supported vehicle types. Weather effects (wind, rain) are applied differently
 * depending on the vehicle selected.
 */
public enum VehicleType {
    CAR,
    SCOOTER,
    BIKE;

    /**
     * Maps string input to VehicleType. Used for controller-level validation.
     */
    public static VehicleType fromString(String type) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Vehicle type cannot be empty");
        }
        try {
            return VehicleType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported vehicle type: " + type);
        }
    }
}