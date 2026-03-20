package com.delivery.fee.service;

import com.delivery.fee.model.City;
import com.delivery.fee.model.FeeRule;
import com.delivery.fee.model.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for handling fee rules in the database
 */
@Repository
public interface FeeRuleRepository extends JpaRepository<FeeRule, Long> {

    List<FeeRule> findByValidToIsNull();

    // Gets all currently active rules for a specific city, vehicle, and time
    @Query("SELECT r FROM FeeRule r WHERE r.vehicleType = :vehicleType AND r.validFrom <= :time AND (r.validTo IS NULL OR r.validTo > :time) AND (r.city IS NULL OR r.city = :city)")
    List<FeeRule> findActiveRules(
            @Param("city") City city,
            @Param("vehicleType") VehicleType vehicleType,
            @Param("time") Instant time);
}