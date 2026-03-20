package com.delivery.fee.controller;

import com.delivery.fee.exception.VehicleForbiddenException;
import com.delivery.fee.model.City;
import com.delivery.fee.model.VehicleType;
import com.delivery.fee.service.DeliveryFeeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DeliveryFeeController.class)
class DeliveryFeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeliveryFeeService deliveryFeeService;

    @Test
    void shouldReturnCalculatedFee_whenRequestIsValid() throws Exception {
        when(deliveryFeeService.calculateDeliveryFee(City.TALLINN, VehicleType.BIKE, null)).thenReturn(new BigDecimal("3.5"));

        mockMvc.perform(get("/api/delivery-fee").param("city", "Tallinn").param("vehicleType", "Bike")).andExpect(status().isOk()).andExpect(jsonPath("$.deliveryFee").value(3.5));
    }

    @Test
    void shouldReturnCalculatedFee_whenDatetimeIsProvided() throws Exception {
        Instant specificTime = Instant.parse("2026-01-01T10:00:00Z");
        when(deliveryFeeService.calculateDeliveryFee(City.TARTU, VehicleType.CAR, specificTime)).thenReturn(new BigDecimal("3.5"));

        mockMvc.perform(get("/api/delivery-fee").param("city", "Tartu").param("vehicleType", "Car").param("datetime", "2026-01-01T10:00:00Z")).andExpect(status().isOk()).andExpect(jsonPath("$.deliveryFee").value(3.5));
    }

    @Test
    void shouldReturnBadRequestWithErrorMessage_whenWeatherIsForbiddenForVehicle() throws Exception {
        when(deliveryFeeService.calculateDeliveryFee(any(City.class), any(VehicleType.class), any())).thenThrow(new VehicleForbiddenException("Usage of selected vehicle type is forbidden"));

        mockMvc.perform(get("/api/delivery-fee").param("city", "Pärnu").param("vehicleType", "Scooter")).andExpect(status().isBadRequest()).andExpect(content().string("Usage of selected vehicle type is forbidden"));
    }

    @Test
    void shouldReturnBadRequest_whenCityIsInvalid() throws Exception {
        mockMvc.perform(get("/api/delivery-fee").param("city", "London").param("vehicleType", "Car")).andExpect(status().isBadRequest()).andExpect(content().string("Unsupported city: London"));
    }
}