package com.maidc.data.entity;

import com.maidc.common.jpa.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDate;

/**
 * 患者主索引实体，列名与 docker/init-db/04-cdr.sql 的 cdr.c_patient DDL 严格对齐
 * （含 10-cdr-patch.sql 补充的 MIMIC anchor 列）。禁止 id_card_hash/phone_hash 等别名词汇。
 */
@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "c_patient", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.c_patient SET is_deleted = true WHERE id = ?")
public class PatientEntity extends BaseEntity {

    @Column(name = "patient_no", nullable = false, length = 64)
    private String patientNo;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    /** DDL CHECK 约束：M/F/O */
    @Column(name = "gender", length = 8)
    private String gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "id_card_no", length = 32)
    private String idCardNo;

    @Column(name = "blood_type", length = 8)
    private String bloodType;

    @Column(name = "phone", length = 32)
    private String phone;

    @Column(name = "address", length = 256)
    private String address;

    @Column(name = "ethnicity", length = 32)
    private String ethnicity;

    @Column(name = "marital_status", length = 16)
    private String maritalStatus;

    @Column(name = "occupation", length = 64)
    private String occupation;

    @Column(name = "source_system", length = 32)
    private String sourceSystem;

    @Column(name = "source_id", length = 64)
    private String sourceId;

    /** MIMIC anchor：死亡日期（10-cdr-patch.sql） */
    @Column(name = "dod")
    private LocalDate dod;

    /** MIMIC anchor：死亡标记 */
    @Column(name = "expire_flag")
    private Boolean expireFlag;

    /** MIMIC anchor：锚定年龄，优先于 birth_date 推导年龄 */
    @Column(name = "anchor_age")
    private Integer anchorAge;
}
