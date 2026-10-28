CREATE TABLE brands (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT uk_brands_name UNIQUE (name)
);

INSERT INTO brands (name) VALUES
    ('Toyota'), ('Renault'), ('Dacia'), ('Peugeot'), ('Mercedes'),
    ('BMW'), ('Audi'), ('Hyundai'), ('Kia'), ('Volkswagen'), ('Ford'),
    ('Fiat'), ('Citroën'), ('Nissan'), ('Skoda'), ('Seat'), ('Opel'),
    ('Mazda'), ('Honda'), ('Volvo'), ('Suzuki'), ('Mitsubishi'), ('Jeep'),
    ('Land Rover'), ('Porsche'), ('Tesla'), ('Chevrolet'), ('Lexus')
ON CONFLICT DO NOTHING;

CREATE TABLE categories (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT uk_categories_name UNIQUE (name)
);

INSERT INTO categories (name) VALUES
    ('CITADINE'), ('BERLINE'), ('SUV'), ('LUXE'), ('UTILITAIRE')
ON CONFLICT DO NOTHING;

INSERT INTO brands (name)
SELECT MIN(TRIM(v.marque))
FROM vehicles v
WHERE TRIM(v.marque) <> ''
  AND NOT EXISTS (
      SELECT 1
      FROM brands b
      WHERE LOWER(b.name) = LOWER(TRIM(v.marque))
  )
GROUP BY LOWER(TRIM(v.marque))
ON CONFLICT DO NOTHING;

INSERT INTO categories (name)
SELECT DISTINCT UPPER(TRIM(categorie))
FROM vehicles
WHERE TRIM(categorie) <> ''
ON CONFLICT DO NOTHING;

ALTER TABLE vehicles ADD COLUMN brand_id BIGINT;
ALTER TABLE vehicles ADD COLUMN category_id BIGINT;

UPDATE vehicles v
SET brand_id = b.id
FROM brands b
WHERE LOWER(TRIM(v.marque)) = LOWER(b.name);

UPDATE vehicles v
SET category_id = c.id
FROM categories c
WHERE UPPER(TRIM(v.categorie)) = c.name;

ALTER TABLE vehicles ALTER COLUMN brand_id SET NOT NULL;
ALTER TABLE vehicles ALTER COLUMN category_id SET NOT NULL;
ALTER TABLE vehicles ADD CONSTRAINT fk_vehicles_brand
    FOREIGN KEY (brand_id) REFERENCES brands (id);
ALTER TABLE vehicles ADD CONSTRAINT fk_vehicles_category
    FOREIGN KEY (category_id) REFERENCES categories (id);

DROP INDEX idx_vehicles_marque;
DROP INDEX idx_vehicles_categorie;
ALTER TABLE vehicles DROP COLUMN marque;
ALTER TABLE vehicles DROP COLUMN categorie;

CREATE INDEX idx_vehicles_brand ON vehicles (brand_id);
CREATE INDEX idx_vehicles_category ON vehicles (category_id);
