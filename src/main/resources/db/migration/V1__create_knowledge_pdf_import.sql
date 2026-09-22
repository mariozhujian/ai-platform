CREATE TABLE IF NOT EXISTS knowledge_import (
    file_hash VARCHAR(64) PRIMARY KEY,
    filename TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

COMMENT ON TABLE knowledge_import IS '知识库文件导入记录';
COMMENT ON COLUMN knowledge_import.file_hash IS '文件内容哈希值，用于文件去重';
COMMENT ON COLUMN knowledge_import.filename IS '文件名称';
COMMENT ON COLUMN knowledge_import.created_at IS '导入记录创建时间';
