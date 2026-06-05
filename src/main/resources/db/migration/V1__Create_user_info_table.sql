CREATE TABLE IF NOT EXISTS user_info (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create index on email for faster lookups
CREATE INDEX IF NOT EXISTS idx_user_info_email ON user_info(email);

-- Add comments
COMMENT ON TABLE user_info IS 'Stores user account information';
COMMENT ON COLUMN user_info.id IS 'Unique identifier (UUID)';
COMMENT ON COLUMN user_info.email IS 'User email address (unique, used for login)';
COMMENT ON COLUMN user_info.name IS 'User full name';
COMMENT ON COLUMN user_info.password_hash IS 'Bcrypt hashed password';
COMMENT ON COLUMN user_info.phone IS 'User phone number (optional)';
COMMENT ON COLUMN user_info.created_at IS 'Account creation timestamp';

