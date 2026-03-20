package com.delivery.fee.service;

import com.delivery.fee.model.FeeRule;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Service class for managing fee rules.
 * Handles the business logic for creating, updating, and soft-deleting rules.
 */
@Service
public class FeeRuleService {

    private final FeeRuleRepository feeRuleRepository;

    public FeeRuleService(FeeRuleRepository feeRuleRepository) {
        this.feeRuleRepository = feeRuleRepository;
    }

    /**
     * Retrieves all rules that are currently active.
     */
    public List<FeeRule> getAllActiveRules() {
        return feeRuleRepository.findByValidToIsNull();
    }

    /**
     * Finds a specific rule by its unique ID.
     */
    public Optional<FeeRule> getRuleById(Long id) {
        return feeRuleRepository.findById(id);
    }

    /**
     * Saves a new fee rule to the database, setting its start time to now.
     */
    @Transactional
    public FeeRule createRule(FeeRule rule) {
        rule.setValidFrom(Instant.now());
        rule.setValidTo(null);
        return feeRuleRepository.save(rule);
    }

    /**
     * Updates an existing rule using a versioning strategy.
     * The old rule is "closed" by setting its validTo timestamp to now,
     * and a new record is created with the updated values.
     */
    @Transactional
    public Optional<FeeRule> updateRule(Long id, FeeRule updatedRule) {
        return feeRuleRepository.findById(id)
                .filter(rule -> rule.getValidTo() == null)
                .map(rule -> {
                    Instant now = Instant.now();
                    rule.setValidTo(now);
                    feeRuleRepository.save(rule);

                    FeeRule newRule = new FeeRule();
                    newRule.setRuleType(updatedRule.getRuleType());
                    newRule.setCity(updatedRule.getCity());
                    newRule.setVehicleType(updatedRule.getVehicleType());
                    newRule.setMinLimit(updatedRule.getMinLimit());
                    newRule.setMaxLimit(updatedRule.getMaxLimit());
                    newRule.setPhenomenonKeyword(updatedRule.getPhenomenonKeyword());
                    newRule.setFee(updatedRule.getFee());
                    newRule.setForbidden(updatedRule.getForbidden());
                    newRule.setValidFrom(now);
                    newRule.setValidTo(null);
                    return feeRuleRepository.save(newRule);
                });
    }

    /**
     * Soft-deletes a rule by setting its validTo timestamp to the current time.
     * Returns true if the rule existed and was successfully deactivated.
     */
    @Transactional
    public boolean deleteRule(Long id) {
        return feeRuleRepository.findById(id)
                .filter(rule -> rule.getValidTo() == null)
                .map(rule -> {
                    rule.setValidTo(Instant.now());
                    feeRuleRepository.save(rule);
                    return true;
                })
                .orElse(false);
    }
}