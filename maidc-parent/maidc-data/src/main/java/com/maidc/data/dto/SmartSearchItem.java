package com.maidc.data.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SmartSearchItem {
    private String domain;
    private Long id;
    private Long patientId;
    private String patientName;
    private String title;
    private String subtitle;
    private double score;
    private String headline;
}
