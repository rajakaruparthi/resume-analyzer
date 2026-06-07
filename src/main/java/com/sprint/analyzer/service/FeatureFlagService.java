package com.sprint.analyzer.service;

import com.sprint.analyzer.entity.FeatureFlag;
import com.sprint.analyzer.entity.req.FeatureFlagRequest;
import com.sprint.analyzer.repo.FeatureFlagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureFlagService {

    public static final String CACHE_NAME = "feature-flags";

    private final FeatureFlagRepository repo;

    public boolean isEnabled(FeatureFlag featureFlag) {
        return isEnabled(featureFlag.getName());
    }

    @Cacheable(value = CACHE_NAME, key = "#name")
    public boolean isEnabled(String name) {
        return repo.findByName(name)
                .map(FeatureFlag::isEnabled)
                .orElseGet(() -> {
                    log.warn("Feature flag '{}' not found — defaulting to OFF", name);
                    return false;
                });
    }

    public List<FeatureFlag> all() {
        return repo.findAll();
    }

    @Transactional
    @CacheEvict(value = CACHE_NAME, key = "#name")
    public FeatureFlag setEnabled(String name, boolean enabled) {
        FeatureFlag flag = repo.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Unknown feature flag: " + name));
        flag.setEnabled(enabled);
        FeatureFlag saved = repo.save(flag);
        log.info("Feature flag '{}' set to {}", name, enabled);
        return saved;
    }

    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public FeatureFlag createOrUpdate(String name, boolean enabled, String description) {
        FeatureFlag flag = repo.findByName(name)
                .orElseGet(() -> FeatureFlag.builder().name(name).build());
        flag.setEnabled(enabled);
        flag.setDescription(description);
        return repo.save(flag);
    }


    @Transactional
    @CacheEvict(value = CACHE_NAME, key = "#req.name")
    public FeatureFlag create(FeatureFlagRequest req) {
        if (repo.existsByName(req.getName())) {
            throw new IllegalArgumentException("Feature flag '" + req.getName() + "' already exists");
        }
        FeatureFlag flag = FeatureFlag.builder()
                .name(req.getName())
                .enabled(Boolean.TRUE.equals(req.getEnabled()))
                .description(req.getDescription())
                .build();
        FeatureFlag saved = repo.save(flag);
        log.info("Feature flag '{}' created (enabled={})", saved.getName(), saved.isEnabled());
        return saved;
    }

    @Transactional
    @CacheEvict(value = CACHE_NAME, key = "#name")
    public FeatureFlag update(String name, FeatureFlagRequest req) {
        FeatureFlag flag = repo.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Unknown feature flag: " + name));

        if (req.getEnabled() != null) flag.setEnabled(req.getEnabled());
        if (req.getDescription() != null) flag.setDescription(req.getDescription());

        FeatureFlag saved = repo.save(flag);
        log.info("Feature flag '{}' updated (enabled={})", saved.getName(), saved.isEnabled());
        return saved;
    }

    @Transactional
    @CacheEvict(value = CACHE_NAME, key = "#name")
    public void delete(String name) {
        FeatureFlag flag = repo.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Unknown feature flag: " + name));
        repo.delete(flag);
        log.warn("Feature flag '{}' deleted", name);
    }

    public FeatureFlag getByName(String name) {
        return repo.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Unknown feature flag: " + name));
    }
}