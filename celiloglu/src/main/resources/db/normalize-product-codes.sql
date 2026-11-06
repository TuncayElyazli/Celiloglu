-- ==============================================================================
-- Celiloglu Backend - Köhnə Məhsul Kodlarının Normallaşdırılması Skripti
-- PostgreSQL Manual Migration Script
-- ==============================================================================
-- DİQQƏT: Bu skript tətbiq tərəfindən avtomatik icra edilmir.
-- Yalnız verilənlər bazası administratoru tərəfindən əl ilə (manual) icra olunmalıdır.
-- Məqsəd: products cədvəlindəki köhnə kiçik hərfli və ya boşluqlu kodları
-- vahid böyük hərf standartına (UPPER(TRIM(code))) gətirmək.
-- OEM kodları, məhsul adları, sifariş və qəbz nömrələri dəyişdirilmir.
-- ==============================================================================

-- ==============================================================================
-- 1-Cİ HİSSƏ: READ-ONLY YOXLAMALAR (Məlumatı dəyişmir)
-- ==============================================================================

-- 1.1. Normallaşdırma sonrası yarana biləcək toqquşmaların (təkrarlanan kodların) yoxlanışı:
-- Əgər bu sorğu hər hansı nəticə qaytarsa, həmin məhsullar manual araşdırılmalıdır.
SELECT 
    UPPER(TRIM(code)) AS normalized_code,
    COUNT(*) AS duplicate_count,
    ARRAY_AGG(id ORDER BY id) AS product_ids,
    ARRAY_AGG(code ORDER BY id) AS original_codes
FROM products
WHERE code IS NOT NULL
GROUP BY UPPER(TRIM(code))
HAVING COUNT(*) > 1;

-- 1.2. Yararsız (NULL və ya yalnız boşluqdan ibarət) kodların yoxlanışı:
SELECT 
    id, 
    name, 
    code AS invalid_code
FROM products
WHERE code IS NULL OR TRIM(code) = '';

-- 1.3. Yenilənməyə namizəd olan (dəyişəcək) məhsul kodlarının siyahısı:
SELECT 
    id,
    name,
    code AS current_code,
    UPPER(TRIM(code)) AS new_normalized_code
FROM products
WHERE code IS NOT NULL
  AND TRIM(code) <> ''
  AND code <> UPPER(TRIM(code))
ORDER BY id;


-- ==============================================================================
-- 2-Cİ HİSSƏ: TRANZAKSİYA VƏ QORUNAN YENİLƏMƏ BLOKU
-- Əgər toqquşma və ya yararsız kod aşkar edilərsə, heç bir dəyişiklik edilmədən
-- xəta ataraq icranı dayandırır və tranzaksiyanı ləğv edir (ROLLBACK).
-- Problem yoxdursa, yalnız dəyişməsi vacib olan products.code dəyərləri yenilənir.
-- Məhsul ID-ləri və bütün xarici əlaqələr (FK) toxunulmaz qalır.
-- ==============================================================================

BEGIN;

DO $$
DECLARE
    v_invalid_count INT;
    v_collision_count INT;
    v_updated_count INT;
BEGIN
    -- 1. Yararsız (NULL və ya boş) kodların yoxlanması
    SELECT COUNT(*) INTO v_invalid_count
    FROM products
    WHERE code IS NULL OR TRIM(code) = '';

    IF v_invalid_count > 0 THEN
        RAISE EXCEPTION 'MƏLUMAT DƏYİŞMƏDƏN DAYANDIRILDI: % ədəd NULL və ya boş məhsul kodu aşkar edildi! Zəhmət olmasa əvvəlcə bu məhsulları düzəldin.', v_invalid_count;
    END IF;

    -- 2. Normallaşdırma sonrası unikal məhdudiyyətini (unique constraint) poza biləcək toqquşmaların yoxlanması
    SELECT COUNT(*) INTO v_collision_count
    FROM (
        SELECT UPPER(TRIM(code))
        FROM products
        GROUP BY UPPER(TRIM(code))
        HAVING COUNT(*) > 1
    ) AS collisions;

    IF v_collision_count > 0 THEN
        RAISE EXCEPTION 'MƏLUMAT DƏYİŞMƏDƏN DAYANDIRILDI: Normallaşdırma sonrası % ədəd toqquşan kod qrupu (kolliziya) aşkar edildi! Əvvəlcə 1-ci hissədəki sorğu ilə toqquşmaları manual təmizləyin.', v_collision_count;
    END IF;

    -- 3. Heç bir toqquşma və xəta yoxdursa, yalnız dəyişməsi vacib olan məhsul kodlarını yenilə
    UPDATE products
    SET code = UPPER(TRIM(code))
    WHERE code IS NOT NULL
      AND TRIM(code) <> ''
      AND code <> UPPER(TRIM(code));

    GET DIAGNOSTICS v_updated_count = ROW_COUNT;

    RAISE NOTICE 'UĞURLU: % ədəd məhsul kodu vahid böyük hərf standartına yeniləndi.', v_updated_count;
END $$;

COMMIT;

