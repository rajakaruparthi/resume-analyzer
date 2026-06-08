package com.sprint.analyzer.entity.req;

import com.sprint.analyzer.annotations.PasswordHash;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name cannot exceed 255 characters")
    private String name;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @PasswordHash // This will hash the password during deserialization
    private String password;

    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    private String phone;

    // Role can be optionally set by admin, default to USER in service
    private Role role;
}