package com.maidc.auth.entity;

import com.maidc.common.jpa.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

@Getter
@Setter
@Entity
@Table(name = "s_config", schema = "system")
@Where(clause = "is_deleted = false")
public class SystemConfigEntity extends BaseEntity {

    @Column(name = "config_key", nullable = false, length = 128)
    private String configKey;

    @Column(name = "config_value", nullable = false, columnDefinition = "TEXT")
    private String configValue;

    @Column(name = "config_type", nullable = false, length = 16)
    private String configType = "STRING";

    @Column(name = "config_group", nullable = false, length = 64)
    private String configGroup = "basic";

    @Column(name = "description", length = 256)
    private String description;

    @Column(name = "is_encrypted", nullable = false)
    private Boolean isEncrypted = false;
}
