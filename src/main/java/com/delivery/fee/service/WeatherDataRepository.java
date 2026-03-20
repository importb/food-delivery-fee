package com.delivery.fee.service;

import com.delivery.fee.model.WeatherData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Interface for saving and retrieving weather observations.
 */
@Repository
public interface WeatherDataRepository extends JpaRepository<WeatherData, Long> {

    Optional<WeatherData> findFirstByStationNameOrderByObservationTimestampDesc(String stationName);

    Optional<WeatherData> findFirstByStationNameAndObservationTimestampLessThanEqualOrderByObservationTimestampDesc(String stationName, Long timestamp);

    boolean existsByStationNameAndObservationTimestamp(String stationName, Long timestamp);
}