package com.maidc.data.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 知识检索结果：FTS 命中行 + ts_headline 高亮片段
 */
@Getter
@Setter
public class DiseaseKbSearchVO {

    private Long id;
    private Long spaceId;
    private String spaceName;
    private String itemType;
    private String title;
    /** 命中片段（含 <em> 高亮） */
    private String snippet;
    private String status;
    private LocalDate publishDate;
}
