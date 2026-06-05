package com.sprint.analyzer.service;

import com.sprint.analyzer.entity.User;
import com.sprint.analyzer.repo.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service layer for user management operations.
 * Handles business logic for user creation, retrieval, update, and deletion.
 */
@Service
@AllArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;

    /**
     * Create a new user.
     *
     * @param user The user object to create
     * @return The created user with generated ID
     * @throws IllegalArgumentException if email already exists
     */
    public User createUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            log.warn("Attempt to create user with existing email: {}", user.getEmail());
            throw new IllegalArgumentException("User with email " + user.getEmail() + " already exists");
        }
        
        log.info("Creating new user with email: {}", user.getEmail());
        return userRepository.save(user);
    }

    /**
     * Retrieve a user by ID.
     *
     * @param id The user's UUID
     * @return Optional containing the user if found
     */
    public Optional<User> getUserById(UUID id) {
        log.info("Retrieving user by ID: {}", id);
        return userRepository.findById(id);
    }

    /**
     * Retrieve a user by email.
     *
     * @param email The user's email address
     * @return Optional containing the user if found
     */
    public Optional<User> getUserByEmail(String email) {
        log.info("Retrieving user by email: {}", email);
        return userRepository.findByEmail(email);
    }

    /**
     * Get all users.
     *
     * @return List of all users
     */
    public List<User> getAllUsers() {
        log.info("Retrieving all users");
        return userRepository.findAll();
    }

    /**
     * Update an existing user.
     *
     * @param id The user's UUID
     * @param updatedUser The updated user data
     * @return The updated user
     * @throws RuntimeException if user not found
     */
    public User updateUser(UUID id, User updatedUser) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));

        // Update fields if provided
        if (updatedUser.getName() != null) {
            user.setName(updatedUser.getName());
        }
        if (updatedUser.getPhone() != null) {
            user.setPhone(updatedUser.getPhone());
        }
        if (updatedUser.getPasswordHash() != null) {
            user.setPasswordHash(updatedUser.getPasswordHash());
        }

        log.info("Updating user with ID: {}", id);
        return userRepository.save(user);
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

    /**
     * Check if a user exists with the given email.
     *
     * @param email The email address to check
     * @return true if user exists, false otherwise
     */
    public boolean userExists(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Get total count of users.
     *
     * @return Number of users in the system
     */
    public long getUserCount() {
        return userRepository.count();
    }

    public Map<String, Object> buildCreateUserResponse(User user) {
        try {
            User createdUser = createUser(user);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User created successfully");
            response.put("userId", createdUser.getId());
            response.put("email", createdUser.getEmail());
            return response;
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
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
        try {
            User updatedUser = updateUser(id, user);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User updated successfully");
            response.put("data", updatedUser);
            return response;
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
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

}
