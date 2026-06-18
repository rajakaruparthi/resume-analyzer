package com.sprint.analyzer.service;

import com.sprint.analyzer.entity.FeatureFlag;
import com.sprint.analyzer.entity.User;
import com.sprint.analyzer.entity.req.RegisterRequest;
import com.sprint.analyzer.entity.req.Role;
import com.sprint.analyzer.repo.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import com.sprint.analyzer.properties.AwsProperties;


@Service
@AllArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FeatureFlagService featureFlagService;
    private final EmailVerificationService emailVerificationService;
    private final AwsProperties awsProperties;

    public User createUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            log.warn("Attempt to create user with existing email: {}", user.getEmail());
            throw new IllegalArgumentException("User with email " + user.getEmail() + " already exists");
        }
        user.setRole(user.getRole() != null ? user.getRole() : Role.USER);
        if (user.getId() == null) {
            user.setId(UUID.randomUUID());
        }
        if (user.getBucketName() == null || user.getBucketName().isBlank()) {
            String baseBucket = awsProperties.getBucketName();
            if (baseBucket == null || baseBucket.isBlank()) {
                user.setBucketName(user.getId().toString().toLowerCase());
            } else {
                user.setBucketName((baseBucket + "-" + user.getId().toString()).toLowerCase());
            }
        }
        User saved = userRepository.save(user);
        log.info("Creating new user with email: {}", user.getEmail());
        if (featureFlagService.isEnabled("email_verification")) {
            emailVerificationService.sendVerificationFor(saved);
        } else {
            user.setEmailVerified(true);
        }
        return saved;
    }

    public User registerUser(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Attempt to register user with existing email: {}", request.getEmail());
            throw new IllegalArgumentException("User with email " + request.getEmail() + " already exists");
        }

        boolean verificationEnabled = featureFlagService.isEnabled("email_verification");

        User user = new User();
        UUID userId = UUID.randomUUID();
        user.setId(userId);
        user.setEmail(request.getEmail());
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        user.setPassword(request.getPassword());
        user.setRole(request.getRole() != null ? request.getRole() : Role.USER);

        String baseBucket = awsProperties.getBucketName();
        if (baseBucket == null || baseBucket.isBlank()) {
            user.setBucketName(userId.toString().toLowerCase());
        } else {
            user.setBucketName((baseBucket + "-" + userId.toString()).toLowerCase());
        }

        log.info("Registered new user with email: {} (verificationEnabled={})", user.getEmail(), verificationEnabled);

        if (verificationEnabled) {
            emailVerificationService.sendVerificationFor(user);
        } else {
            user.setEmailVerified(true);
        }

        return userRepository.save(user);
    }


    public User authenticateUser(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Authentication attempt with non-existent email: {}", email);
                    return new RuntimeException("Invalid email or password"); // Generic message for security
                });

        if (featureFlagService.isEnabled("email_verification") && !user.isEmailVerified()) {
            log.warn("Authentication attempt with unverified email: {}", email);
            throw new RuntimeException("Email not verified. Please check your inbox.");
        }

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            log.warn("Invalid password attempt for email: {}", email);
            throw new RuntimeException("Invalid email or password"); // Generic message for security
        }

        log.info("User authenticated successfully: {}", email);
        return user;
    }

    public Optional<User> getUserById(UUID id) {
        log.info("Retrieving user by ID: {}", id);
        return userRepository.findById(id);
    }

    public Optional<User> getUserByEmail(String email) {
        log.info("Retrieving user by email: {}", email);
        return userRepository.findByEmail(email);
    }

    public List<User> getAllUsers() {
        log.info("Retrieving all users");
        return userRepository.findAll();
    }

    public User updateUser(UUID id, User updatedUser) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));

        if (updatedUser.getName() != null) {
            user.setName(updatedUser.getName());
        }
        if (updatedUser.getPhone() != null) {
            user.setPhone(updatedUser.getPhone());
        }
        if (updatedUser.getPassword() != null) {
            user.setPassword(updatedUser.getPassword());
        }

        log.info("Updating user with ID: {}", id);
        return userRepository.save(user);
    }

    public void updateBucketName(String bucketName, User user) {
        user.setBucketName(bucketName);
        log.info("Updating bucket name for user with ID: {}", user.getId());
        userRepository.save(user);
    }

    /**
     * Delete a user by ID.
     *
     * @param id The user's UUID
     * @return true if user was deleted, false if not found
     */
    public boolean deleteUser(UUID id) {
        if (userRepository.existsById(id)) {
            log.info("Deleting user with ID: {}", id);
            userRepository.deleteById(id);
            return true;
        }
        log.warn("User not found for deletion with ID: {}", id);
        return false;
    }

    public boolean userExists(String email) {
        return userRepository.existsByEmail(email);
    }


    public long getUserCount() {
        return userRepository.count();
    }

    public Map<String, Object> buildCreateUserResponse(User user) {
        Map<String, Object> response = new HashMap<>();
        try {
            User createdUser = createUser(user);
            response.put("success", true);
            response.put("message", "User created successfully");
            response.put("userId", createdUser.getId());
            response.put("email", createdUser.getEmail());
            return response;
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            throw e;
        }
    }

    public Map<String, Object> buildGetUserByIdResponse(UUID id) {
        Optional<User> user = getUserById(id);
        Map<String, Object> response = new HashMap<>();
        if (user.isPresent()) {
            response.put("success", true);
            response.put("data", user.get());
        } else {
            response.put("success", false);
            response.put("error", "User not found");
        }
        return response;
    }

    public Map<String, Object> buildGetUserByEmailResponse(String email) {
        Optional<User> user = getUserByEmail(email);
        Map<String, Object> response = new HashMap<>();
        if (user.isPresent()) {
            response.put("success", true);
            response.put("data", user.get());
        } else {
            response.put("success", false);
            response.put("error", "User not found");
        }
        return response;
    }

    public Map<String, Object> buildGetAllUsersResponse() {
        List<User> users = getAllUsers();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("count", users.size());
        response.put("data", users);
        return response;
    }

    public Map<String, Object> buildUpdateUserResponse(UUID id, User user) {
        Map<String, Object> response = new HashMap<>();
        try {
            User updatedUser = updateUser(id, user);
            response.put("success", true);
            response.put("message", "User updated successfully");
            response.put("data", updatedUser);
            return response;
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            throw e;
        }
    }

    public Map<String, Object> buildDeleteUserResponse(UUID id) {
        boolean deleted = deleteUser(id);
        Map<String, Object> response = new HashMap<>();
        if (deleted) {
            response.put("success", true);
            response.put("message", "User deleted successfully");
        } else {
            response.put("success", false);
            response.put("error", "User not found");
        }
        return response;
    }

    public Map<String, Object> buildCheckEmailExistsResponse(String email) {
        boolean exists = userExists(email);
        Map<String, Object> response = new HashMap<>();
        response.put("email", email);
        response.put("exists", exists);
        return response;
    }

    public Map<String, Object> buildGetUserCountResponse() {
        long count = getUserCount();
        Map<String, Object> response = new HashMap<>();
        response.put("totalUsers", count);
        return response;
    }

    public User loginUser(String email, String password) {
        Optional<User> userOpt = getUserByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            if (!user.isEmailVerified()) {
                log.warn("Login attempt with unverified email: {}", email);
                throw new RuntimeException("Email not verified. Please check your inbox.");
            }
        }
        if (userOpt.isEmpty()) {
            log.warn("Login attempt with non-existent email: {}", email);
            throw new RuntimeException("Invalid email or password");
        }

        User user = userOpt.get();
        if (!verifyPassword(email, password)) {
            log.warn("Invalid password attempt for email: {}", email);
            throw new RuntimeException("Invalid email or password");
        }

        log.info("User logged in successfully: {}", email);
        return user;
    }

    // UserService
    public boolean verifyPassword(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

}
