package com.maidc.data.entity;

import com.maidc.common.jpa.base.BaseEntity;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.Map;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "c_disease_kb_item", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.c_disease_kb_item SET is_deleted = true WHERE id = ?")
public class DiseaseKbItemEntity extends BaseEntity {

    @Column(name = "space_id", nullable = false)
    private Long spaceId;

    /** GUIDELINE / LITERATURE / PATHWAY / SCALE */
    @Column(name = "item_type", nullable = false, length = 32)
    private String itemType;

    @Column(name = "title", nullable = false, length = 512)
    private String title;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_extract", columnDefinition = "jsonb")
    private JsonNode aiExtract;

    /** PENDING / DONE / FAILED */
    @Column(name = "ai_status", nullable = false, length = 16)
    private String aiStatus = "PENDING";

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "source", length = 256)
    private String source;

    @Column(name = "authors", length = 512)
    private String authors;

    @Column(name = "publish_date")
    private LocalDate publishDate;

    @Column(name = "version_no", length = 32)
    private String versionNo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", columnDefinition = "jsonb")
    private Map<String, Object> tags;

    @Column(name = "file_url", length = 512)
    private String fileUrl;

    @Column(name = "file_name", length = 256)
    private String fileName;

    /** DRAFT / PUBLISHED / ARCHIVED */
    @Column(name = "status", nullable = false, length = 16)
    private String status = "DRAFT";

    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;
}
