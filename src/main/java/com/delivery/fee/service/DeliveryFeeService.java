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

        WeatherData weather = weatherDataRepository
                .findFirstByStationNameAndObservationTimestampLessThanEqualOrderByObservationTimestampDesc(
                        city.getStationName(), evalTime.getEpochSecond())
                .orElseThrow(() -> new RuntimeException("No weather data available for " + city + " at " + evalTime));

        List<FeeRule> activeRules = feeRuleRepository.findActiveRules(city, vehicleType, evalTime);

        BigDecimal totalFee = fetchBaseFee(activeRules, city, vehicleType);

        totalFee = totalFee.add(calculateNumericExtra(activeRules, RuleType.AIR_TEMPERATURE, weather.getAirTemperature()));
        totalFee = totalFee.add(calculateNumericExtra(activeRules, RuleType.WIND_SPEED, weather.getWindSpeed()));

        totalFee = totalFee.add(calculatePhenomenonExtra(activeRules, weather.getWeatherPhenomenon()));

        return totalFee;
    }

    private BigDecimal calculateNumericExtra(List<FeeRule> rules, RuleType type, Double value) {
        if (value == null) return BigDecimal.ZERO;

        return rules.stream()
                .filter(r -> r.getRuleType() == type && isWithinRange(value, r.getMinLimit(), r.getMaxLimit()))
                .map(this::checkForbiddenAndReturnFee)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal calculatePhenomenonExtra(List<FeeRule> rules, String phenomenon) {
        if (phenomenon == null || phenomenon.isBlank()) return BigDecimal.ZERO;

        String lowerPhenomenon = phenomenon.toLowerCase();
        return rules.stream()
                .filter(r -> r.getRuleType() == RuleType.WEATHER_PHENOMENON &&
                        r.getPhenomenonKeyword() != null &&
                        lowerPhenomenon.contains(r.getPhenomenonKeyword().toLowerCase()))
                .map(this::checkForbiddenAndReturnFee)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal fetchBaseFee(List<FeeRule> rules, City city, VehicleType vehicleType) {
        return rules.stream()
                .filter(r -> r.getRuleType() == RuleType.BASE_FEE && city.equals(r.getCity()))
                .findFirst()
                .map(FeeRule::getFee)
                .orElseThrow(() -> new IllegalArgumentException("No base fee for " + city + "/" + vehicleType));
    }

    private BigDecimal checkForbiddenAndReturnFee(FeeRule rule) {
        if (Boolean.TRUE.equals(rule.getForbidden())) {
            throw new VehicleForbiddenException("Usage of selected vehicle type is forbidden due to weather conditions");
        }
        return rule.getFee() != null ? rule.getFee() : BigDecimal.ZERO;
    }

    /**
     * Helper method to determine if a value falls within optional min/max boundaries.
     */
    private boolean isWithinRange(Double value, Double min, Double max) {
        if (min != null && value < min) return false;
        return max == null || value <= max;
    }
}