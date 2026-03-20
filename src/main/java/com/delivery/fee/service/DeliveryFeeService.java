package com.delivery.fee.service;

import com.delivery.fee.exception.VehicleForbiddenException;
import com.delivery.fee.model.City;
import com.delivery.fee.model.FeeRule;
import com.delivery.fee.model.RuleType;
import com.delivery.fee.model.VehicleType;
import com.delivery.fee.model.WeatherData;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Service containing the dynamic business rules to calculate the delivery fee.
 */
@Service
public class DeliveryFeeService {

    private final WeatherDataRepository weatherDataRepository;
    private final FeeRuleRepository feeRuleRepository;

    // Injecting repositories to fetch weather observations and dynamic rules from the DB
    public DeliveryFeeService(WeatherDataRepository weatherDataRepository, FeeRuleRepository feeRuleRepository) {
        this.weatherDataRepository = weatherDataRepository;
        this.feeRuleRepository = feeRuleRepository;
    }

    /**
     * Calculates the total delivery fee by summing up the base fee and all applicable weather extra fees.
     */
    public BigDecimal calculateDeliveryFee(City city, VehicleType vehicleType, Instant datetime) {
        Instant evalTime = (datetime != null) ? datetime : Instant.now();
        Long evalTimestamp = evalTime.getEpochSecond();

        WeatherData latestWeatherData = weatherDataRepository
                .findFirstByStationNameAndObservationTimestampLessThanEqualOrderByObservationTimestampDesc(city.getStationName(), evalTimestamp)
                .orElseThrow(() -> new RuntimeException("No weather data found for city: " + city.name() + " at the specified time"));

        List<FeeRule> activeRules = feeRuleRepository.findActiveRules(city, vehicleType, evalTime);

        BigDecimal totalDeliveryFee = calculateRegionalBaseFee(activeRules, city, vehicleType);

        totalDeliveryFee = totalDeliveryFee.add(
                calculateAirTemperatureExtraFee(activeRules, latestWeatherData.getAirTemperature())
        );

        totalDeliveryFee = totalDeliveryFee.add(
                calculateWindSpeedExtraFee(activeRules, latestWeatherData.getWindSpeed())
        );

        totalDeliveryFee = totalDeliveryFee.add(
                calculateWeatherPhenomenonExtraFee(activeRules, latestWeatherData.getWeatherPhenomenon())
        );

        return totalDeliveryFee;
    }

    private BigDecimal calculateRegionalBaseFee(List<FeeRule> rules, City city, VehicleType vehicleType) {
        return rules.stream()
                .filter(r -> r.getRuleType() == RuleType.BASE_FEE && city.equals(r.getCity()))
                .findFirst()
                .map(FeeRule::getFee)
                .orElseThrow(() -> new IllegalArgumentException("Unknown base fee mapping for city: " + city + " and vehicle: " + vehicleType));
    }

    private BigDecimal calculateAirTemperatureExtraFee(List<FeeRule> rules, Double airTemperature) {
        if (airTemperature == null) return BigDecimal.ZERO;

        BigDecimal maxFee = BigDecimal.ZERO;
        for (FeeRule rule : rules) {
            if (rule.getRuleType() == RuleType.AIR_TEMPERATURE && matchesLimit(airTemperature, rule.getMinLimit(), rule.getMaxLimit())) {
                if (Boolean.TRUE.equals(rule.getForbidden())) {
                    throw new VehicleForbiddenException("Usage of selected vehicle type is forbidden");
                }
                if (rule.getFee() != null && rule.getFee().compareTo(maxFee) > 0) {
                    maxFee = rule.getFee();
                }
            }
        }
        return maxFee;
    }

    private BigDecimal calculateWindSpeedExtraFee(List<FeeRule> rules, Double windSpeed) {
        if (windSpeed == null) return BigDecimal.ZERO;

        BigDecimal maxFee = BigDecimal.ZERO;
        for (FeeRule rule : rules) {
            if (rule.getRuleType() == RuleType.WIND_SPEED && matchesLimit(windSpeed, rule.getMinLimit(), rule.getMaxLimit())) {
                if (Boolean.TRUE.equals(rule.getForbidden())) {
                    throw new VehicleForbiddenException("Usage of selected vehicle type is forbidden");
                }
                if (rule.getFee() != null && rule.getFee().compareTo(maxFee) > 0) {
                    maxFee = rule.getFee();
                }
            }
        }
        return maxFee;
    }

    private BigDecimal calculateWeatherPhenomenonExtraFee(List<FeeRule> rules, String weatherPhenomenon) {
        if (weatherPhenomenon == null || weatherPhenomenon.isBlank()) {
            return BigDecimal.ZERO;
        }

        String normalizedPhenomenon = weatherPhenomenon.toLowerCase();
        BigDecimal maxFee = BigDecimal.ZERO;

        for (FeeRule rule : rules) {
            if (rule.getRuleType() == RuleType.WEATHER_PHENOMENON && rule.getPhenomenonKeyword() != null && normalizedPhenomenon.contains(rule.getPhenomenonKeyword().toLowerCase())) {
                if (Boolean.TRUE.equals(rule.getForbidden())) {
                    throw new VehicleForbiddenException("Usage of selected vehicle type is forbidden");
                }
                if (rule.getFee() != null && rule.getFee().compareTo(maxFee) > 0) {
                    maxFee = rule.getFee();
                }
            }
        }

        return maxFee;
    }

    /**
     * Helper method to determine if a value falls within optional min/max boundaries.
     */
    private boolean matchesLimit(Double value, Double minLimit, Double maxLimit) {
        if (minLimit == null && maxLimit != null) {
            return value < maxLimit;
        } else if (minLimit != null && maxLimit == null) {
            return value > minLimit;
        } else if (minLimit != null) {
            return value >= minLimit && value <= maxLimit;
        }
        return false;
    }
}