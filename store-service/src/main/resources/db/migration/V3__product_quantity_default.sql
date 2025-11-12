-- V3__product_quantity_default.sql

-- 1) Подстрахуемся: если вдруг есть NULL в quantity — проставим 0
UPDATE product
SET quantity = 0
WHERE quantity IS NULL;

-- 2) Зададим дефолт для новых вставок
ALTER TABLE product
    ALTER COLUMN quantity SET DEFAULT 0;

-- 3) Оставляем/гарантируем NOT NULL (на случай, если где-то сняли)
ALTER TABLE product
    ALTER COLUMN quantity SET NOT NULL;

-- 4) (Опционально, но полезно) защитим от отрицательных остатков
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'product_quantity_non_negative'
  ) THEN
ALTER TABLE product
    ADD CONSTRAINT product_quantity_non_negative
        CHECK (quantity >= 0);
END IF;
END $$;
