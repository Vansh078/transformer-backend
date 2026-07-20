-- Authentication Module: track how each user authenticates (email/password vs Google via
-- Supabase OAuth) and their organization, so the app can manage profiles independently of
-- the Supabase Auth provider.
ALTER TABLE app_users ADD COLUMN auth_provider VARCHAR(20) NOT NULL DEFAULT 'EMAIL';
ALTER TABLE app_users ADD COLUMN organization VARCHAR(150);
