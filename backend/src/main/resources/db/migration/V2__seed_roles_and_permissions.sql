INSERT INTO permissions (name) VALUES
 ('USER_READ'), ('USER_CREATE'), ('USER_UPDATE'), ('USER_DELETE'), ('PROFILE_READ');

INSERT INTO roles (name) VALUES ('ROLE_USER'), ('ROLE_ADMIN');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.name IN ('USER_READ','PROFILE_READ')
WHERE r.name = 'ROLE_USER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'ROLE_ADMIN';