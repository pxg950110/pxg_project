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

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "r_genomic_variant", schema = "rdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE rdr.r_genomic_variant SET is_deleted = true WHERE id = ?")
public class GenomicVariantEntity extends BaseEntity {

    @Column(name = "genomic_dataset_id", nullable = false)
    private Long genomicDatasetId;

    @Column(name = "chromosome", nullable = false, length = 16)
    private String chromosome;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Column(name = "ref_allele", length = 64)
    private String refAllele;

    @Column(name = "alt_allele", length = 64)
    private String altAllele;

    @Column(name = "variant_type", nullable = false, length = 32)
    private String variantType;

    @Column(name = "gene_symbol", length = 32)
    private String geneSymbol;

    @Column(name = "significance", length = 32)
    private String significance;
}
