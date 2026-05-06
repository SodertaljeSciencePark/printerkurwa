CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS printers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255),
    ip VARCHAR(255) UNIQUE NOT NULL,
    access_code VARCHAR(255),
    model_type VARCHAR(255),
    serial VARCHAR(255)
);