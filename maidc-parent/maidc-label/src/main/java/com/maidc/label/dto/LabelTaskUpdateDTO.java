package com.maidc.label.dto;

import lombok.Data;

import java.util.List;

@Data
public class LabelTaskUpdateDTO {

    private String name;

    private String assigneeId;

    private String status;

    private List<String> labels;

    private String guidelines;
}
