package com.delivery.fee.service;

import com.delivery.fee.model.City;
import com.delivery.fee.model.WeatherData;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Service responsible for fetching weather data from the external portal,
 * parsing the XML response, and persisting relevant station data to the database.
 */
@Service
public class WeatherDataService {

    private static final Logger logger = LoggerFactory.getLogger(WeatherDataService.class);

    private final WeatherDataRepository weatherDataRepository;
    private final String weatherObservationUrl;
    private final RestTemplate restTemplate;
    private final XmlMapper xmlMapper;

    public WeatherDataService(
            WeatherDataRepository weatherDataRepository,
            @Value("${weather.observations.url}") String weatherObservationUrl) {
        this.weatherDataRepository = weatherDataRepository;
        this.weatherObservationUrl = weatherObservationUrl;
        this.restTemplate = new RestTemplate();
        this.xmlMapper = new XmlMapper();
    }

    /**
     * Periodically called to sync local database with external weather service.
     */
    @Transactional
    @Retryable(
            retryFor = RuntimeException.class,
            backoff = @Backoff(delay = 2000, multiplier = 2.0)
    )
    public void importWeatherData() {
        try {
            String xmlResponse = restTemplate.getForObject(weatherObservationUrl, String.class);
            if (xmlResponse == null) {
                logger.warn("Received empty response from weather API");
                return;
            }

            // Map XML to internal DTO classes
            Observations observations = xmlMapper.readValue(xmlResponse, Observations.class);

            if (observations == null || observations.getStations() == null) {
                logger.warn("Invalid XML format: 'observations' tag or stations not found");
                return;
            }

            Long observationTimestamp = observations.getTimestamp();

            // Get names of stations we care about from our City configuration
            List<String> targetStations = Arrays.stream(City.values())
                    .map(City::getStationName)
                    .toList();

            List<WeatherData> parsedDataBatch = new ArrayList<>();

            for (Station station : observations.getStations()) {
                if (targetStations.contains(station.getName())) {
                    if (weatherDataRepository.existsByStationNameAndObservationTimestamp(station.getName(), observationTimestamp)) {
                        continue;
                    }

                    WeatherData weatherData = new WeatherData();
                    weatherData.setStationName(station.getName());
                    weatherData.setWmoCode(station.getWmocode());
                    weatherData.setObservationTimestamp(observationTimestamp);
                    weatherData.setWeatherPhenomenon(station.getPhenomenon());

                    try {
                        if (station.getAirtemperature() != null && !station.getAirtemperature().isBlank()) {
                            weatherData.setAirTemperature(Double.parseDouble(station.getAirtemperature()));
                        }
                        if (station.getWindspeed() != null && !station.getWindspeed().isBlank()) {
                            weatherData.setWindSpeed(Double.parseDouble(station.getWindspeed()));
                        }
                    } catch (NumberFormatException e) {
                        logger.warn("Failed to parse numerical weather data for station {}: {}", station.getName(), e.getMessage());
                    }

                    parsedDataBatch.add(weatherData);
                }
            }

            // Save elements
            if (!parsedDataBatch.isEmpty()) {
                weatherDataRepository.saveAll(parsedDataBatch);
            }
            logger.info("Successfully imported latest weather data.");

        } catch (Exception exception) {
            logger.error("Failed to import weather data. It will be retried on the next schedule: {}", exception.getMessage(), exception);
            throw new RuntimeException("Weather data import failed, rolling back transaction", exception);
        }
    }

    /**
     * DTO representing the root <observations> XML element.
     */
    @JacksonXmlRootElement(localName = "observations")
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Observations {
        @JacksonXmlProperty(isAttribute = true)
        private Long timestamp;

        @JacksonXmlProperty(localName = "station")
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<Station> stations;

        public Long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Long timestamp) {
            this.timestamp = timestamp;
        }

        public List<Station> getStations() {
            return stations;
        }

        public void setStations(List<Station> stations) {
            this.stations = stations;
        }
    }

    /**
     * DTO representing individual <station> elements.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Station {
        private String name;
        private String wmocode;
        private String phenomenon;
        private String airtemperature;
        private String windspeed;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getWmocode() {
            return wmocode;
        }

        public void setWmocode(String wmocode) {
            this.wmocode = wmocode;
        }

        public String getPhenomenon() {
            return phenomenon;
        }

        public void setPhenomenon(String phenomenon) {
            this.phenomenon = phenomenon;
        }

        public String getAirtemperature() {
            return airtemperature;
        }

        public void setAirtemperature(String airtemperature) {
            this.airtemperature = airtemperature;
        }

        public String getWindspeed() {
            return windspeed;
        }

        public void setWindspeed(String windspeed) {
            this.windspeed = windspeed;
        }
    }
}