package com.delivery.fee.scheduler;

import com.delivery.fee.service.WeatherDataService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler component responsible for periodic weather data imports.
 */
@Component
public class WeatherCronJob {

    private final WeatherDataService weatherDataService;

    public WeatherCronJob(WeatherDataService weatherDataService) {
        this.weatherDataService = weatherDataService;
    }

    /**
     * Automatically imports weather data right after the application starts.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void importDataOnStartup() {
        try {
            weatherDataService.importWeatherData();
        } catch (Exception ignored) {
        }
    }

    /**
     * Periodic task to refresh weather data.
     */
    @Scheduled(cron = "${weather.cron.expression}")
    @SchedulerLock(name = "importScheduledWeatherDataLock", lockAtMostFor = "10m", lockAtLeastFor = "1m")
    public void importScheduledWeatherData() {
        weatherDataService.importWeatherData();
    }
}