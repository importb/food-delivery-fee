package com.delivery.fee.model;

import java.math.BigDecimal;

/**
 * Simple DTO for the final calculated delivery fee.
 * BigDecimal is used to ensure precision for monetary values.
 */
public class DeliveryFeeResponse {
    private BigDecimal deliveryFee;

    public DeliveryFeeResponse(BigDecimal deliveryFee) {
        this.deliveryFee = deliveryFee;
    }

    public BigDecimal getDeliveryFee() {
        return deliveryFee;
    }

    public void setDeliveryFee(BigDecimal deliveryFee) {
        this.deliveryFee = deliveryFee;
    }
}