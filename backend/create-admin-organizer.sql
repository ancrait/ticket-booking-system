-- Admin user: admin@tiq.local / admin123
-- Organizer user: organizer@tiq.local / organizer123

INSERT INTO users (id, email, password, first_name, last_name, phone, role, email_verified, created_at, updated_at)
VALUES
    ('282434ab-f214-4592-9833-351e0be22e62',
     'admin@tiq.local',
     '$2a$10$D9lSLknUVCcNN72Xjvzgou8yUG61GtrWSjwXBcpCLs/DCfEfFaiMW',
     'Admin',
     'User',
     '+380000000000',
     'ADMIN',
     true,
     now(),
     now()),
    ('68e3a800-4be2-4817-8d80-cd7478cb8952',
     'organizer@tiq.local',
     '$2a$10$glNI8XIDhCZKKvut3bqeee3QLdpFK7zPXeMUmfVNJbhaorAD63HRi',
     'Event',
     'Organizer',
     '+380000000001',
     'ORGANIZER',
     true,
     now(),
     now())
ON CONFLICT (email) DO NOTHING;
