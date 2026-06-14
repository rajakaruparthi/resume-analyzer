package com.sprint.analyzer.model;

import lombok.Data;

import java.util.List;

@Data
public class Project {

    private String name;

    private String description;

    private List<String> technologies;

    private List<String> bullets;

    private String githubUrl;

    private String projectUrl;
}
