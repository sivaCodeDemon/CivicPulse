-- V2__seed_initial_data.sql: Standard municipal roles and departments

INSERT INTO roles (id, name, description) VALUES
(1, 'ROLE_CITIZEN', 'Citizen user with complaint reporting and status tracking permissions'),
(2, 'ROLE_OFFICER', 'Municipal department officer who can review, assign, and resolve complaints'),
(3, 'ROLE_ADMIN', 'System administrator with platform configuration and analytics permissions')
ON CONFLICT (id) DO NOTHING;

INSERT INTO departments (id, name, code, description, default_sla_hours) VALUES
(1, 'Road Infrastructure & Public Works', 'ROADS_PWD', 'Potholes, damaged pavements, road cave-ins, and divider repairs', 48),
(2, 'Solid Waste Management & Sanitation', 'SANITATION_SWM', 'Garbage overflow, illegal dumping, street sweeping, and bin clearance', 24),
(3, 'Water Supply & Sewerage Board', 'WATER_SEWER', 'Water pipe bursts, low pressure, contaminated water, and open manholes', 36),
(4, 'Electrical & Street Lighting', 'ELECTRIC_LIGHT', 'Non-functional streetlights, dangling live wires, and transformer issues', 24),
(5, 'Parks & Urban Horticulture', 'PARKS_URBAN', 'Fallen tree branches, park maintenance, and overgrown foliage', 72)
ON CONFLICT (id) DO NOTHING;
