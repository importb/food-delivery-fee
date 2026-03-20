package com.delivery.fee.model;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Enumeration of supported cities.
 * Maps internal city enums to specific weather station names used by the external weather service.
 */
public enum City {
    TALLINN("Tallinn-Harku"),
    TARTU("Tartu-Tõravere"),
    PARNU("Pärnu");

    private final String stationName;

    // Pre-calculated list of station names for efficient lookup/filtering during weather imports
    public static final List<String> ALL_STATION_NAMES = Arrays.stream(values())
            .map(City::getStationName)
            .collect(Collectors.toList());

    City(String stationName) {
        this.stationName = stationName;
    }

    public String getStationName() {
        return stationName;
    }

    /**
     * Converts raw string input into a City enum.
     * Handles case-insensitivity and specific character mapping for Pärnu.
     */
    public static City fromString(String cityName) {
        if (cityName == null || cityName.isBlank()) {
            throw new IllegalArgumentException("City name cannot be empty");
        }

        String normalizedCityName = cityName.trim().toUpperCase();
        if (normalizedCityName.equals("PÄRNU") || normalizedCityName.equals("PARNU")) {
            return PARNU;
        }

        try {
            return City.valueOf(normalizedCityName);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported city: " + cityName);
        }
    }
}