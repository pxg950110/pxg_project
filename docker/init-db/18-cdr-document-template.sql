-- ==================== CDR: Document Template ====================

CREATE TABLE IF NOT EXISTS cdr.c_document_template (
    id              BIGSERIAL    PRIMARY KEY,
    code            VARCHAR(64)  NOT NULL,
    name            VARCHAR(128) NOT NULL,
    note_type       VARCHAR(64)  NOT NULL,
    description     TEXT,
    schema_def      JSONB,
    version         INT          NOT NULL DEFAULT 1,
    status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    created_by      VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_document_template_code UNIQUE (code)
);

COMMENT ON TABLE cdr.c_document_template IS 'Document templates for clinical notes';
COMMENT ON COLUMN cdr.c_document_template.schema_def IS 'JSON Schema for validating structured_data in clinical notes';

CREATE INDEX IF NOT EXISTS idx_c_doctmpl_note_type ON cdr.c_document_template(note_type);

-- Seed templates
INSERT INTO cdr.c_document_template (code, name, note_type, description, schema_def, status, is_deleted, org_id) VALUES
('admission-note', 'Admission Note Template', 'ADMISSION', 'Admission record template',
 '{"type":"object","properties":{"chiefComplaint":{"type":"string"},"presentIllness":{"type":"string"},"pastHistory":{"type":"string"},"physicalExam":{"type":"string"}}}',
 'ACTIVE', false, 0),
('operation-note', 'Operation Note Template', 'OPERATION', 'Surgical operation record template',
 '{"type":"object","properties":{"preOpDiagnosis":{"type":"string"},"postOpDiagnosis":{"type":"string"},"procedureName":{"type":"string"},"anesthesiaType":{"type":"string"},"operativeFindings":{"type":"string"}}}',
 'ACTIVE', false, 0),
('discharge-note', 'Discharge Summary Template', 'DISCHARGE', 'Discharge summary template',
 '{"type":"object","properties":{"admissionDiagnosis":{"type":"string"},"dischargeDiagnosis":{"type":"string"},"treatmentSummary":{"type":"string"},"dischargeInstructions":{"type":"string"},"followUpPlan":{"type":"string"}}}',
 'ACTIVE', false, 0);
