CREATE TABLE IF NOT EXISTS resume_metadata (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    original_filename TEXT NOT NULL,
    s3_key TEXT,
    file_type VARCHAR(20),
    token_count INT,
    status VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_resume_user
        FOREIGN KEY (user_id)
        REFERENCES user_info(id) ON DELETE CASCADE
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_resume_metadata_user_id ON resume_metadata(user_id);
CREATE INDEX IF NOT EXISTS idx_resume_metadata_s3_key ON resume_metadata(s3_key);
CREATE INDEX IF NOT EXISTS idx_resume_metadata_status ON resume_metadata(status);
CREATE INDEX IF NOT EXISTS idx_resume_metadata_created_at ON resume_metadata(created_at);

-- Add table and column comments
COMMENT ON TABLE resume_metadata IS 'Stores metadata for uploaded resumes including S3 location and processing status';
COMMENT ON COLUMN resume_metadata.id IS 'Unique identifier (UUID)';
COMMENT ON COLUMN resume_metadata.user_id IS 'Foreign key reference to user_info table';
COMMENT ON COLUMN resume_metadata.original_filename IS 'Original filename as uploaded by user';
COMMENT ON COLUMN resume_metadata.s3_key IS 'S3 object key/path for the file';
COMMENT ON COLUMN resume_metadata.file_type IS 'File type/MIME type (e.g., pdf, docx)';
COMMENT ON COLUMN resume_metadata.token_count IS 'Number of tokens in the document (for LLM processing)';
COMMENT ON COLUMN resume_metadata.status IS 'Processing status (PENDING, PROCESSING, COMPLETED, FAILED)';
COMMENT ON COLUMN resume_metadata.created_at IS 'Timestamp when resume was uploaded';

