package com.maidc.data.etl;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Maps CSV file/directory names to ODS table names for MIMIC-III and MIMIC-IV data import.
 * <p>
 * MIMIC-III tables use the {@code o3_} prefix, CSV files reside under {@code iii/}.
 * MIMIC-IV tables use the {@code o4_} prefix, CSV files reside under {@code iv/}.
 * <p>
 * Column names are NOT hardcoded here — they are dynamically queried from
 * {@code information_schema.columns} at runtime, excluding metadata columns
 * (id, _batch_id, _source_file, _loaded_at, _row_hash, _is_valid).
 */
@Component
public class TableMapping {

    /** All 57 table mapping entries. */
    @Getter
    private List<TableEntry> entries;

    @PostConstruct
    void init() {
        entries = List.of(

            // ===== MIMIC-III (o3_) — CSV base dir: iii/ =====

            new TableEntry("o3_caregivers",          "CAREGIVERS.csv",          DataSource.MIMIC_III, false),
            new TableEntry("o3_d_items",             "D_ITEMS.csv",             DataSource.MIMIC_III, false),
            new TableEntry("o3_d_labitems",          "D_LABITEMS.csv",          DataSource.MIMIC_III, false),
            new TableEntry("o3_d_icd_diagnoses",     "D_ICD_DIAGNOSES.csv",     DataSource.MIMIC_III, false),
            new TableEntry("o3_d_icd_procedures",    "D_ICD_PROCEDURES.csv",    DataSource.MIMIC_III, false),
            new TableEntry("o3_d_cpt",               "D_CPT.csv",               DataSource.MIMIC_III, false),
            new TableEntry("o3_patients",            "PATIENTS.csv",            DataSource.MIMIC_III, false),
            new TableEntry("o3_admissions",          "ADMISSIONS.csv",          DataSource.MIMIC_III, false),
            new TableEntry("o3_icustays",            "ICUSTAYS.csv",            DataSource.MIMIC_III, false),
            new TableEntry("o3_services",            "SERVICES.csv",            DataSource.MIMIC_III, false),
            new TableEntry("o3_transfers",           "TRANSFERS.csv",           DataSource.MIMIC_III, false),
            new TableEntry("o3_diagnoses_icd",       "DIAGNOSES_ICD.csv",       DataSource.MIMIC_III, false),
            new TableEntry("o3_procedures_icd",      "PROCEDURES_ICD.csv",      DataSource.MIMIC_III, false),
            new TableEntry("o3_drgcodes",            "DRGCODES.csv",            DataSource.MIMIC_III, false),
            new TableEntry("o3_cptevents",           "CPTEVENTS.csv",           DataSource.MIMIC_III, false),
            new TableEntry("o3_callout",             "CALLOUT.csv",             DataSource.MIMIC_III, false),
            new TableEntry("o3_chartevents",         "CHARTEVENTS.csv",         DataSource.MIMIC_III, true),  // ~33GB
            new TableEntry("o3_labevents",           "LABEVENTS.csv",           DataSource.MIMIC_III, true),  // ~1.8GB
            new TableEntry("o3_prescriptions",       "PRESCRIPTIONS.csv",       DataSource.MIMIC_III, true),  // ~735MB
            new TableEntry("o3_inputevents_cv",      "INPUTEVENTS_CV.csv",      DataSource.MIMIC_III, true),  // ~2.3GB
            new TableEntry("o3_inputevents_mv",      "INPUTEVENTS_MV.csv",      DataSource.MIMIC_III, true),  // ~931MB
            new TableEntry("o3_outputevents",        "OUTPUTEVENTS.csv",        DataSource.MIMIC_III, false), // ~396MB
            new TableEntry("o3_noteevents",          "NOTEEVENTS.csv",          DataSource.MIMIC_III, true),  // ~3.8GB
            new TableEntry("o3_microbiologyevents",  "MICROBIOLOGYEVENTS.csv",  DataSource.MIMIC_III, false),
            new TableEntry("o3_datetimeevents",      "DATETIMEEVENTS.csv",      DataSource.MIMIC_III, true),  // ~526MB
            new TableEntry("o3_procedureevents_mv",  "PROCEDUREEVENTS_MV.csv",  DataSource.MIMIC_III, false),

            // ===== MIMIC-IV (o4_) — CSV base dir: iv/ =====

            new TableEntry("o4_patients",            "patients.csv",            DataSource.MIMIC_IV, false),
            new TableEntry("o4_admissions",          "admissions.csv",          DataSource.MIMIC_IV, false),
            new TableEntry("o4_provider",            "provider.csv",            DataSource.MIMIC_IV, false),
            new TableEntry("o4_d_hcpcs",             "d_hcpcs.csv",             DataSource.MIMIC_IV, false),
            new TableEntry("o4_d_icd_diagnoses",     "d_icd_diagnoses.csv",     DataSource.MIMIC_IV, false),
            new TableEntry("o4_d_icd_procedures",    "d_icd_procedures.csv",    DataSource.MIMIC_IV, false),
            new TableEntry("o4_d_labitems",          "d_labitems.csv",          DataSource.MIMIC_IV, false),
            new TableEntry("o4_d_items",             "d_items.csv",             DataSource.MIMIC_IV, false),
            new TableEntry("o4_diagnoses_icd",       "diagnoses_icd.csv",       DataSource.MIMIC_IV, false),
            new TableEntry("o4_procedures_icd",      "procedures_icd.csv",      DataSource.MIMIC_IV, false),
            new TableEntry("o4_prescriptions",       "prescriptions.csv",       DataSource.MIMIC_IV, true),  // ~735MB
            new TableEntry("o4_pharmacy",            "pharmacy.csv",            DataSource.MIMIC_IV, false),
            new TableEntry("o4_emar",                "emar.csv",                DataSource.MIMIC_IV, true),  // ~2.6GB
            new TableEntry("o4_emar_detail",         "emar_detail.csv",         DataSource.MIMIC_IV, true),  // ~4.3GB
            new TableEntry("o4_poe",                 "poe.csv",                 DataSource.MIMIC_IV, true),
            new TableEntry("o4_poe_detail",          "poe_detail.csv",          DataSource.MIMIC_IV, true),
            new TableEntry("o4_labevents",           "labevents.csv",           DataSource.MIMIC_IV, true),  // ~5.5GB
            new TableEntry("o4_microbiologyevents",  "microbiologyevents.csv",  DataSource.MIMIC_IV, false),
            new TableEntry("o4_transfers",           "transfers.csv",           DataSource.MIMIC_IV, false),
            new TableEntry("o4_services",            "services.csv",            DataSource.MIMIC_IV, false),
            new TableEntry("o4_drgcodes",            "drgcodes.csv",            DataSource.MIMIC_IV, false),
            new TableEntry("o4_chartevents",         "chartevents.csv",         DataSource.MIMIC_IV, true),  // ~42GB
            new TableEntry("o4_datetimeevents",      "datetimeevents.csv",      DataSource.MIMIC_IV, true),
            new TableEntry("o4_procedureevents",     "procedureevents.csv",     DataSource.MIMIC_IV, true),
            new TableEntry("o4_outputevents",        "outputevents.csv",        DataSource.MIMIC_IV, true),
            new TableEntry("o4_inputevents",         "inputevents.csv",         DataSource.MIMIC_IV, true),
            new TableEntry("o4_icustays",            "icustays.csv",            DataSource.MIMIC_IV, false),
            new TableEntry("o4_noteevents",          "noteevents.csv",          DataSource.MIMIC_IV, true),
            new TableEntry("o4_note",                "note.csv",                DataSource.MIMIC_IV, false),
            new TableEntry("o4_omr",                 "omr.csv",                 DataSource.MIMIC_IV, false),
            new TableEntry("o4_hcpcsevents",         "hcpcsevents.csv",         DataSource.MIMIC_IV, false)
        );
    }

    /**
     * Returns all 57 table mapping entries (unmodifiable).
     */
    public List<TableEntry> getAll() {
        return Collections.unmodifiableList(entries);
    }

    /**
     * Filters entries by data source.
     *
     * @param source the data source to filter by
     * @return unmodifiable list of matching entries
     */
    public List<TableEntry> getBySource(DataSource source) {
        return entries.stream()
                .filter(e -> e.source() == source)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Finds a single entry by its ODS table name.
     *
     * @param tableName the ODS table name (e.g. "o3_patients")
     * @return the matching entry, or empty if not found
     */
    public Optional<TableEntry> getByTableName(String tableName) {
        return entries.stream()
                .filter(e -> e.tableName().equals(tableName))
                .findFirst();
    }

    // ===== Inner types =====

    /**
     * Data source identifier for MIMIC datasets.
     */
    public enum DataSource {
        /** MIMIC-III — CSV files under iii/, ODS tables prefixed with o3_ */
        MIMIC_III,
        /** MIMIC-IV — CSV files under iv/, ODS tables prefixed with o4_ */
        MIMIC_IV
    }

    /**
     * A single table-to-CSV mapping entry.
     *
     * @param tableName   the ODS table name (e.g. "o3_chartevents")
     * @param csvFileName the CSV file/directory name (e.g. "CHARTEVENTS.csv")
     * @param source      the data source (MIMIC_III or MIMIC_IV)
     * @param largeTable  true if the table is expected to be >= 500 MB
     */
    public record TableEntry(
            String tableName,
            String csvFileName,
            DataSource source,
            boolean largeTable
    ) {}
}
