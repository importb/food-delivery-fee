package com.delivery.fee.controller;

import com.delivery.fee.model.FeeRule;
import com.delivery.fee.service.FeeRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for managing delivery fee rules.
 * Provides endpoints to list, create, update, and delete rules.
 */
@RestController
@RequestMapping("/api/rules")
@Tag(name = "Fee Rule Management API", description = "CRUD operations for delivery fee rules mapping")
public class FeeRuleController {

    private final FeeRuleService feeRuleService;

    public FeeRuleController(FeeRuleService feeRuleService) {
        this.feeRuleService = feeRuleService;
    }

    @Operation(summary = "Get all active fee rules")
    @GetMapping
    public List<FeeRule> getAllRules() {
        return feeRuleService.getAllActiveRules();
    }

    @Operation(summary = "Get a fee rule by ID")
    @GetMapping("/{id}")
    public ResponseEntity<FeeRule> getRuleById(@PathVariable Long id) {
        return feeRuleService.getRuleById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Create a new fee rule")
    @PostMapping
    public ResponseEntity<FeeRule> createRule(@RequestBody FeeRule rule) {
        FeeRule createdRule = feeRuleService.createRule(rule);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRule);
    }

    @Operation(summary = "Update an existing fee rule.")
    @PutMapping("/{id}")
    public ResponseEntity<FeeRule> updateRule(@PathVariable Long id, @RequestBody FeeRule updatedRule) {
        return feeRuleService.updateRule(id, updatedRule)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Delete a fee rule")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        if (feeRuleService.deleteRule(id)) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}