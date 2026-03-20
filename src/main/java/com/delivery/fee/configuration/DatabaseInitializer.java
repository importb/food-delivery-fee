package com.delivery.fee.configuration;

import com.delivery.fee.model.City;
import com.delivery.fee.model.FeeRule;
import com.delivery.fee.model.RuleType;
import com.delivery.fee.model.VehicleType;
import com.delivery.fee.service.FeeRuleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Initializes the database dynamically with the default fee rules.
 */
@Configuration
public class DatabaseInitializer {

    @Bean
    CommandLineRunner initDatabase(FeeRuleRepository feeRuleRepository) {
        return args -> {
            if (feeRuleRepository.count() == 0) {
                createBaseFee(feeRuleRepository, City.TALLINN, VehicleType.CAR, "4.0");
                createBaseFee(feeRuleRepository, City.TALLINN, VehicleType.SCOOTER, "3.5");
                createBaseFee(feeRuleRepository, City.TALLINN, VehicleType.BIKE, "3.0");

                createBaseFee(feeRuleRepository, City.TARTU, VehicleType.CAR, "3.5");
                createBaseFee(feeRuleRepository, City.TARTU, VehicleType.SCOOTER, "3.0");
                createBaseFee(feeRuleRepository, City.TARTU, VehicleType.BIKE, "2.5");

                createBaseFee(feeRuleRepository, City.PARNU, VehicleType.CAR, "3.0");
                createBaseFee(feeRuleRepository, City.PARNU, VehicleType.SCOOTER, "2.5");
                createBaseFee(feeRuleRepository, City.PARNU, VehicleType.BIKE, "2.0");

                for (VehicleType vt : List.of(VehicleType.SCOOTER, VehicleType.BIKE)) {
                    createTempRule(feeRuleRepository, vt, null, -10.0, "1.0");
                    createTempRule(feeRuleRepository, vt, -10.0, 0.0, "0.5");
                }

                createWindRule(feeRuleRepository, VehicleType.BIKE, 10.0, 20.0, "0.5", false);
                createWindRule(feeRuleRepository, VehicleType.BIKE, 20.0, null, "0.0", true);

                for (VehicleType vt : List.of(VehicleType.SCOOTER, VehicleType.BIKE)) {
                    createPhenomenonRule(feeRuleRepository, vt, "snow", "1.0", false);
                    createPhenomenonRule(feeRuleRepository, vt, "sleet", "1.0", false);
                    createPhenomenonRule(feeRuleRepository, vt, "rain", "0.5", false);
                    createPhenomenonRule(feeRuleRepository, vt, "shower", "0.5", false);

                    createPhenomenonRule(feeRuleRepository, vt, "glaze", "0.0", true);
                    createPhenomenonRule(feeRuleRepository, vt, "hail", "0.0", true);
                    createPhenomenonRule(feeRuleRepository, vt, "thunder", "0.0", true);
                }
            }
        };
    }

    /**
     * Creates a base fee rule for a specific city and vehicle type.
     */
    private void createBaseFee(FeeRuleRepository repo, City city, VehicleType vehicleType, String fee) {
        FeeRule rule = new FeeRule();
        rule.setRuleType(RuleType.BASE_FEE);
        rule.setCity(city);
        rule.setVehicleType(vehicleType);
        rule.setFee(new BigDecimal(fee));
        rule.setForbidden(false);
        rule.setValidFrom(Instant.EPOCH);
        repo.save(rule);
    }

    /**
     * Creates a temperature-based rule.
     */
    private void createTempRule(FeeRuleRepository repo, VehicleType vehicleType, Double minLimit, Double maxLimit, String fee) {
        FeeRule rule = new FeeRule();
        rule.setRuleType(RuleType.AIR_TEMPERATURE);
        rule.setVehicleType(vehicleType);
        rule.setMinLimit(minLimit);
        rule.setMaxLimit(maxLimit);
        rule.setFee(new BigDecimal(fee));
        rule.setForbidden(false);
        rule.setValidFrom(Instant.EPOCH);
        repo.save(rule);
    }

    /**
     * Creates a wind-speed rule.
     */
    private void createWindRule(FeeRuleRepository repo, VehicleType vehicleType, Double minLimit, Double maxLimit, String fee, boolean forbidden) {
        FeeRule rule = new FeeRule();
        rule.setRuleType(RuleType.WIND_SPEED);
        rule.setVehicleType(vehicleType);
        rule.setMinLimit(minLimit);
        rule.setMaxLimit(maxLimit);
        rule.setFee(new BigDecimal(fee));
        rule.setForbidden(forbidden);
        rule.setValidFrom(Instant.EPOCH);
        repo.save(rule);
    }

    /**
     * Creates a weather phenomenon rule based on keyword matching.
     */
    private void createPhenomenonRule(FeeRuleRepository repo, VehicleType vehicleType, String keyword, String fee, boolean forbidden) {
        FeeRule rule = new FeeRule();
        rule.setRuleType(RuleType.WEATHER_PHENOMENON);
        rule.setVehicleType(vehicleType);
        rule.setPhenomenonKeyword(keyword);
        rule.setFee(new BigDecimal(fee));
        rule.setForbidden(forbidden);
        rule.setValidFrom(Instant.EPOCH);
        repo.save(rule);
    }
}