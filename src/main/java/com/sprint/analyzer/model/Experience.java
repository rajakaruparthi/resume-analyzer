package com.sprint.analyzer.model;

import lombok.Data;

import java.util.List;

@Data
public class Experience {

    private String company;

    private String role;

    private String location;

    private String duration;

    private String startDate;

    private String endDate;

    private Boolean currentEmployer;

    private List<String> bullets;
}
