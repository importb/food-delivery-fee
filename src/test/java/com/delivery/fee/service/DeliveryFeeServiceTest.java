package com.delivery.fee.service;

import com.delivery.fee.exception.VehicleForbiddenException;
import com.delivery.fee.model.City;
import com.delivery.fee.model.FeeRule;
import com.delivery.fee.model.RuleType;
import com.delivery.fee.model.VehicleType;
import com.delivery.fee.model.WeatherData;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryFeeServiceTest {

    @Mock
    private WeatherDataRepository weatherDataRepository;

    @Mock
    private FeeRuleRepository feeRuleRepository;

    @InjectMocks
    private DeliveryFeeService deliveryFeeService;

    @BeforeEach
    void setUp() {
        FeeRule windForbidden = new FeeRule();
        windForbidden.setRuleType(RuleType.WIND_SPEED);
        windForbidden.setMinLimit(20.0);
        windForbidden.setForbidden(true);

        FeeRule windExtra = new FeeRule();
        windExtra.setRuleType(RuleType.WIND_SPEED);
        windExtra.setMinLimit(10.0);
        windExtra.setMaxLimit(20.0);
        windExtra.setFee(new BigDecimal("0.5"));
        windExtra.setForbidden(false);

        FeeRule tempExtra1 = new FeeRule();
        tempExtra1.setRuleType(RuleType.AIR_TEMPERATURE);
        tempExtra1.setMaxLimit(-10.0);
        tempExtra1.setFee(new BigDecimal("1.0"));
        tempExtra1.setForbidden(false);

        FeeRule tempExtra2 = new FeeRule();
        tempExtra2.setRuleType(RuleType.AIR_TEMPERATURE);
        tempExtra2.setMinLimit(-10.0);
        tempExtra2.setMaxLimit(0.0);
        tempExtra2.setFee(new BigDecimal("0.5"));
        tempExtra2.setForbidden(false);

        FeeRule phenomSnow = new FeeRule();
        phenomSnow.setRuleType(RuleType.WEATHER_PHENOMENON);
        phenomSnow.setPhenomenonKeyword("snow");
        phenomSnow.setFee(new BigDecimal("1.0"));
        phenomSnow.setForbidden(false);

        FeeRule phenomThunder = new FeeRule();
        phenomThunder.setRuleType(RuleType.WEATHER_PHENOMENON);
        phenomThunder.setPhenomenonKeyword("thunder");
        phenomThunder.setForbidden(true);

        when(feeRuleRepository.findActiveRules(any(City.class), any(VehicleType.class), any(Instant.class))).thenAnswer(invocation -> {
            City city = invocation.getArgument(0);
            VehicleType vt = invocation.getArgument(1);

            List<FeeRule> rules = new ArrayList<>();

            FeeRule baseRule = getFeeRule(city, vt);
            rules.add(baseRule);

            if (vt == VehicleType.BIKE) {
                rules.add(windExtra);
                rules.add(windForbidden);
                rules.add(tempExtra1);
                rules.add(tempExtra2);
                rules.add(phenomSnow);
                rules.add(phenomThunder);
            } else if (vt == VehicleType.SCOOTER) {
                rules.add(tempExtra1);
                rules.add(tempExtra2);
                rules.add(phenomSnow);
                rules.add(phenomThunder);
            }
            return rules;
        });
    }

    private static @NonNull FeeRule getFeeRule(City city, VehicleType vt) {
        FeeRule baseRule = new FeeRule();
        baseRule.setRuleType(RuleType.BASE_FEE);
        baseRule.setCity(city);
        baseRule.setVehicleType(vt);
        if (city == City.TALLINN && vt == VehicleType.CAR) baseRule.setFee(new BigDecimal("4.0"));
        else if (city == City.TALLINN && vt == VehicleType.BIKE) baseRule.setFee(new BigDecimal("3.0"));
        else if (city == City.TARTU && vt == VehicleType.BIKE) baseRule.setFee(new BigDecimal("2.5"));
        else if (city == City.PARNU && vt == VehicleType.SCOOTER) baseRule.setFee(new BigDecimal("2.5"));
        else if (city == City.TARTU && vt == VehicleType.SCOOTER) baseRule.setFee(new BigDecimal("3.0"));
        else baseRule.setFee(new BigDecimal("0.0"));
        return baseRule;
    }

    @Test
    void shouldReturnOnlyBaseFee_whenVehicleIsCar() {
        WeatherData weatherData = new WeatherData();
        weatherData.setAirTemperature(-15.0);
        weatherData.setWindSpeed(25.0);
        weatherData.setWeatherPhenomenon("Glaze");

        when(weatherDataRepository.findFirstByStationNameAndObservationTimestampLessThanEqualOrderByObservationTimestampDesc(anyString(), any(Long.class))).thenReturn(Optional.of(weatherData));

        BigDecimal fee = deliveryFeeService.calculateDeliveryFee(City.TALLINN, VehicleType.CAR, null);
        assertEquals(new BigDecimal("4.0"), fee);
    }

    @Test
    void shouldApplyMultipleWeatherExtraFees_whenVehicleIsBikeInHarshWeather() {
        WeatherData weatherData = new WeatherData();
        weatherData.setAirTemperature(-2.1);
        weatherData.setWindSpeed(4.7);
        weatherData.setWeatherPhenomenon("Light snow shower");

        when(weatherDataRepository.findFirstByStationNameAndObservationTimestampLessThanEqualOrderByObservationTimestampDesc(eq(City.TARTU.getStationName()), any(Long.class))).thenReturn(Optional.of(weatherData));

        BigDecimal fee = deliveryFeeService.calculateDeliveryFee(City.TARTU, VehicleType.BIKE, null);
        assertEquals(new BigDecimal("4.0"), fee);
    }

    @Test
    void shouldApplyTemperatureExtraFee_whenScooterOperatesInExtremeCold() {
        WeatherData weatherData = new WeatherData();
        weatherData.setAirTemperature(-12.0);
        weatherData.setWindSpeed(15.0);

        when(weatherDataRepository.findFirstByStationNameAndObservationTimestampLessThanEqualOrderByObservationTimestampDesc(eq(City.PARNU.getStationName()), any(Long.class))).thenReturn(Optional.of(weatherData));

        BigDecimal fee = deliveryFeeService.calculateDeliveryFee(City.PARNU, VehicleType.SCOOTER, null);
        assertEquals(new BigDecimal("3.5"), fee);
    }

    @Test
    void shouldThrowVehicleForbiddenException_whenWindSpeedExceedsLimitForBike() {
        WeatherData weatherData = new WeatherData();
        weatherData.setWindSpeed(21.0);

        when(weatherDataRepository.findFirstByStationNameAndObservationTimestampLessThanEqualOrderByObservationTimestampDesc(anyString(), any(Long.class))).thenReturn(Optional.of(weatherData));

        VehicleForbiddenException exception = assertThrows(VehicleForbiddenException.class, () -> deliveryFeeService.calculateDeliveryFee(City.TALLINN, VehicleType.BIKE, null));

        assertEquals("Usage of selected vehicle type is forbidden due to weather conditions", exception.getMessage());
    }

    @Test
    void shouldThrowVehicleForbiddenException_whenWeatherPhenomenonIsThunderstorm() {
        WeatherData weatherData = new WeatherData();
        weatherData.setWeatherPhenomenon("Thunderstorm");

        when(weatherDataRepository.findFirstByStationNameAndObservationTimestampLessThanEqualOrderByObservationTimestampDesc(anyString(), any(Long.class))).thenReturn(Optional.of(weatherData));

        VehicleForbiddenException exception = assertThrows(VehicleForbiddenException.class, () -> deliveryFeeService.calculateDeliveryFee(City.TARTU, VehicleType.SCOOTER, null));

        assertEquals("Usage of selected vehicle type is forbidden due to weather conditions", exception.getMessage());
    }

    @Test
    void shouldCalculateFeeUsingHistoricalData_whenSpecificDatetimeIsProvided() {
        Instant historicalTime = Instant.parse("2026-03-20T10:00:00Z");
        WeatherData weatherData = new WeatherData();
        weatherData.setAirTemperature(-15.0);
        weatherData.setWindSpeed(5.0);

        when(weatherDataRepository.findFirstByStationNameAndObservationTimestampLessThanEqualOrderByObservationTimestampDesc(eq(City.TALLINN.getStationName()), eq(historicalTime.getEpochSecond()))).thenReturn(Optional.of(weatherData));

        BigDecimal fee = deliveryFeeService.calculateDeliveryFee(City.TALLINN, VehicleType.CAR, historicalTime);
        assertEquals(new BigDecimal("4.0"), fee);
    }
}