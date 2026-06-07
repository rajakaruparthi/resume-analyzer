package com.sprint.analyzer.controller;

import com.sprint.analyzer.entity.FeatureFlag;
import com.sprint.analyzer.entity.req.FeatureFlagRequest;
import com.sprint.analyzer.service.FeatureFlagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/feature-flags")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Feature Flags", description = "Admin APIs to manage runtime feature toggles")
public class FeatureFlagController {

    private final FeatureFlagService featureFlagService;

    @GetMapping
    @Operation(summary = "List all feature flags")
    public ResponseEntity<List<FeatureFlag>> list() {
        return ResponseEntity.ok(featureFlagService.all());
    }

    @GetMapping("/{name}")
    @Operation(summary = "Get a feature flag by name")
    public ResponseEntity<FeatureFlag> get(@PathVariable String name) {
        try {
            return ResponseEntity.ok(featureFlagService.getByName(name));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @Operation(summary = "Create a new feature flag")
    public ResponseEntity<?> create(@Valid @RequestBody FeatureFlagRequest req) {
        try {
            FeatureFlag created = featureFlagService.create(req);
            return ResponseEntity
                    .created(URI.create("/api/admin/feature-flags/" + created.getName()))
                    .body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PutMapping("/{name}")
    @Operation(summary = "Update an existing feature flag (full or partial)")
    public ResponseEntity<?> update(@PathVariable String name,
                                    @Valid @RequestBody FeatureFlagRequest req) {
        try {
            return ResponseEntity.ok(featureFlagService.update(name, req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PatchMapping("/{name}/toggle")
    @Operation(summary = "Toggle a feature flag on/off (shortcut for setting `enabled`)")
    public ResponseEntity<?> toggle(@PathVariable String name,
                                    @RequestBody Map<String, Boolean> body) {
        Boolean enabled = body.get("enabled");
        if (enabled == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "`enabled` is required"));
        }
        try {
            return ResponseEntity.ok(featureFlagService.setEnabled(name, enabled));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @DeleteMapping("/{name}")
    @Operation(summary = "Delete a feature flag")
    public ResponseEntity<?> delete(@PathVariable String name) {
        try {
            featureFlagService.delete(name);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}