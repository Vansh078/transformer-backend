-- Phase 2 AI features: multi-turn "Ask Your Transformer" chat history
CREATE TABLE chat_conversations (
    id BIGSERIAL PRIMARY KEY,
    transformer_id BIGINT REFERENCES transformers(id) ON DELETE CASCADE,
    user_id UUID REFERENCES app_users(id) ON DELETE SET NULL,
    title VARCHAR(200),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_chat_conversations_transformer ON chat_conversations (transformer_id, updated_at);

CREATE TABLE chat_messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES chat_conversations(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL, -- SYSTEM, USER, ASSISTANT
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_chat_messages_conversation ON chat_messages (conversation_id, created_at);

-- Explainable AI + Automatic Incident Narratives: attach LLM-authored context to alerts
ALTER TABLE alerts ADD COLUMN narrative TEXT;
ALTER TABLE alerts ADD COLUMN explanation TEXT;

-- Phase 4: Activity Logs (audit trail across the platform)
CREATE TABLE activity_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID REFERENCES app_users(id) ON DELETE SET NULL,
    actor_label VARCHAR(150),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(50),
    details VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_activity_logs_created ON activity_logs (created_at);
CREATE INDEX idx_activity_logs_entity ON activity_logs (entity_type, entity_id);
