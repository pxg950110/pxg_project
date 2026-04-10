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

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "c_patient", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.c_patient SET is_deleted = true WHERE id = ?")
public class PatientEntity extends BaseEntity {

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "gender", length = 8)
    private String gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "id_card_hash", length = 128)
    private String idCardHash;

    @Column(name = "phone_hash", length = 128)
    private String phoneHash;

    @Column(name = "address", length = 256)
    private String address;
}
