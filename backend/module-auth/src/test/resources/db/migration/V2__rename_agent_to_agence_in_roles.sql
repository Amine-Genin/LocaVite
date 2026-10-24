-- Migration V2 : Renommage du rôle AGENT en AGENCE et garantie d'existence des rôles CLIENT, AGENCE et ADMIN

UPDATE roles SET name = 'AGENCE' WHERE name = 'AGENT';

INSERT INTO roles (name)
SELECT 'CLIENT' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'CLIENT');

INSERT INTO roles (name)
SELECT 'AGENCE' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'AGENCE');

INSERT INTO roles (name)
SELECT 'ADMIN' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ADMIN');
