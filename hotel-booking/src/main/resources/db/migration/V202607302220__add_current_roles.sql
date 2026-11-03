INSERT INTO roles (code, name, description)
VALUES
    ('ADMIN', 'Quản trị viên', 'Quản trị toàn bộ hệ thống'),
    ('HOUSEKEEPER', 'Nhân viên buồng phòng', 'Quản lý công việc buồng phòng')
ON CONFLICT (code) DO UPDATE
SET
    name = EXCLUDED.name,
    description = EXCLUDED.description;

UPDATE roles
SET
    name = 'Quản lý',
    description = 'Quản lý hoạt động khách sạn'
WHERE code = 'MANAGER';

UPDATE roles
SET
    name = 'Lễ tân',
    description = 'Quản lý đặt phòng và khách lưu trú'
WHERE code = 'RECEPTIONIST';
