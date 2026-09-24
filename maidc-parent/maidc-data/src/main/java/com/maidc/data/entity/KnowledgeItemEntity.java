package com.maidc.data.entity;

import com.maidc.common.jpa.base.BaseEntity;
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
@Table(name = "m_knowledge_item", schema = "masterdata")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE masterdata.m_knowledge_item SET is_deleted = true WHERE id = ?")
public class KnowledgeItemEntity extends BaseEntity {

    @Column(name = "title", nullable = false, length = 512)
    private String title;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "item_type", nullable = false, length = 32)
    private String itemType = "GUIDELINE";

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "source", length = 256)
    private String source;

    @Column(name = "authors", length = 512)
    private String authors;

    @Column(name = "publish_date")
    private LocalDate publishDate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", columnDefinition = "jsonb")
    private Map<String, Object> tags;

    @Column(name = "file_url", length = 512)
    private String fileUrl;

    @Column(name = "file_name", length = 256)
    private String fileName;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "DRAFT";
}
