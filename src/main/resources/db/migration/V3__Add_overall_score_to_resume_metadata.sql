ALTER TABLE resume_metadata ADD COLUMN overall_score INT;
COMMENT ON COLUMN resume_metadata.overall_score IS 'Overall resume score parsed by LLM';
