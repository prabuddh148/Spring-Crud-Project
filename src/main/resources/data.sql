-- Seed roles. AuthService looks up the USER role when registering,
-- so this must exist before the first /auth/register call.
-- INSERT IGNORE keeps this safe to re-run on every startup.
INSERT IGNORE INTO roles (name) VALUES ('USER');
INSERT IGNORE INTO roles (name) VALUES ('ADMIN');
