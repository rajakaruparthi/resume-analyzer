package com.sprint.analyzer.entity.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FeatureFlagRequest {

    @NotBlank(message = "name is required")
    @Size(max = 100, message = "name must be ≤ 100 characters")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]*$",
            message = "name must be UPPER_SNAKE_CASE (e.g. EMAIL_VERIFICATION_ENABLED)")
    private String name;

    @NotNull(message = "enabled is required")
    private Boolean enabled;

    @Size(max = 500, message = "description must be ≤ 500 characters")
    private String description;
}