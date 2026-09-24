package com.maidc.data.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllergyVO {
    private Long id;
    private String allergen;
    private String allergenType;
    private String reaction;
    private String severity;
    private LocalDateTime confirmedAt;
}
