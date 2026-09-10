CREATE TABLE IF NOT EXISTS knowledge_pdf_import (
    file_hash VARCHAR(64) PRIMARY KEY,
    filename TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
