package com.delivery.fee.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Main entity for dynamic business rules.
 * Allows modifying fee components (Base fee, Weather extras) via DB without code changes.
 */
@Entity
@Table(name = "fee_rule")
public class FeeRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Identifies if this is a Base Fee, or a specific weather-related surcharge
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RuleType ruleType;

    @Enumerated(EnumType.STRING)
    private City city;

    @Enumerated(EnumType.STRING)
    private VehicleType vehicleType;

    // Range-based conditions (e.g., Wind Speed between 10 and 20 m/s)
    private Double minLimit;
    private Double maxLimit;
    private String phenomenonKeyword;

    private BigDecimal fee;

    // If true, certain weather conditions may completely ban specific vehicle types
    @Column(nullable = false)
    private Boolean forbidden = false;

    // Audit/Versioning fields to track when rules were applied
    @Column(nullable = false)
    private Instant validFrom = Instant.now();

    @Column
    private Instant validTo;

    public FeeRule() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RuleType getRuleType() {
        return ruleType;
    }

    public void setRuleType(RuleType ruleType) {
        this.ruleType = ruleType;
    }

    public City getCity() {
        return city;
    }

    public void setCity(City city) {
        this.city = city;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
    }

    public Double getMinLimit() {
        return minLimit;
    }

    public void setMinLimit(Double minLimit) {
        this.minLimit = minLimit;
    }

    public Double getMaxLimit() {
        return maxLimit;
    }

    public void setMaxLimit(Double maxLimit) {
        this.maxLimit = maxLimit;
    }

    public String getPhenomenonKeyword() {
        return phenomenonKeyword;
    }

    public void setPhenomenonKeyword(String phenomenonKeyword) {
        this.phenomenonKeyword = phenomenonKeyword;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee;
    }

    public Boolean getForbidden() {
        return forbidden;
    }

    public void setForbidden(Boolean forbidden) {
        this.forbidden = forbidden;
    }

    public Instant getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Instant validFrom) {
        this.validFrom = validFrom;
    }

    public Instant getValidTo() {
        return validTo;
    }

    public void setValidTo(Instant validTo) {
        this.validTo = validTo;
    }
}