package com.delivery.fee.model;

/**
 * Categorizes how a specific fee rule should be applied during calculation.
 */
public enum RuleType {
    BASE_FEE,           // The starting fee based on City + Vehicle
    AIR_TEMPERATURE,    // Extra fee based on cold/heat
    WIND_SPEED,         // Extra fee based on wind (mostly for bikes)
    WEATHER_PHENOMENON  // Extra fee or usage ban based on precipitation/visibility
}