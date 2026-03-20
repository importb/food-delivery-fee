package com.delivery.fee.controller;

import com.delivery.fee.model.City;
import com.delivery.fee.model.DeliveryFeeResponse;
import com.delivery.fee.model.VehicleType;
import com.delivery.fee.service.DeliveryFeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Main entry point for the delivery fee calculation request.
 */
@RestController
@RequestMapping("/api/delivery-fee")
@Tag(name = "Delivery Fee Calculation API", description = "Endpoints for calculating food delivery fees based on weather conditions")
public class DeliveryFeeController {

    private final DeliveryFeeService deliveryFeeService;

    public DeliveryFeeController(DeliveryFeeService deliveryFeeService) {
        this.deliveryFeeService = deliveryFeeService;
    }

    /**
     * Endpoint to get the delivery fee.
     * It takes the city and vehicle as strings and handles the conversion to internal Enums.
     */
    @Operation(summary = "Calculate Delivery Fee", description = "Calculates the total delivery fee based on the regional base fee, vehicle type, and the weather conditions at the requested time.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully calculated delivery fee",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = DeliveryFeeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters or usage of selected vehicle type is forbidden",
                    content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)))
    })
    @GetMapping
    public ResponseEntity<DeliveryFeeResponse> getDeliveryFee(
            @Parameter(description = "The target city for delivery (Tallinn, Tartu, Pärnu)", required = true, example = "Tallinn")
            @RequestParam("city") String city,

            @Parameter(description = "The type of vehicle used for delivery (Car, Scooter, Bike)", required = true, example = "Bike")
            @RequestParam("vehicleType") String vehicleType,

            @Parameter(description = "Optional datetime to calculate fee for a specific time (ISO-8601 format)", required = false, example = "2026-03-20T10:46:00Z")
            @RequestParam(value = "datetime", required = false) Instant datetime) {

        City parsedCity = City.fromString(city);
        VehicleType parsedVehicleType = VehicleType.fromString(vehicleType);

        BigDecimal deliveryFee = deliveryFeeService.calculateDeliveryFee(parsedCity, parsedVehicleType, datetime);

        return ResponseEntity.ok(new DeliveryFeeResponse(deliveryFee));
    }
}