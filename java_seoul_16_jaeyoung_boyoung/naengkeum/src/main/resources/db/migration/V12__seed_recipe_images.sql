-- =====================================================================
--  V12__seed_recipe_images.sql
--  초기 시드 공개 레시피(ID 1~10)의 대표 이미지 백필.
--
--  배경: V4 시드 레시피 중 ID 1~10은 image_url이 비어 있었다.
--        이미지 파일은 naengkeum/uploads/recipe/seed-N.* 로 저장(레포에 포함),
--        정적 서빙 경로 /images/recipe/seed-N.* 로 노출된다.
--        (app.upload.dir=./uploads, public-path=/images → /images/recipe/x)
--
--  주의: 사용자가 올린 레시피(author_id IS NOT NULL)는 건드리지 않는다.
--        대상은 ID 1~10 한정.
-- =====================================================================

UPDATE recipe SET image_url = '/images/recipe/seed-1.webp' WHERE recipe_id = 1;
UPDATE recipe SET image_url = '/images/recipe/seed-2.jpg'  WHERE recipe_id = 2;
UPDATE recipe SET image_url = '/images/recipe/seed-3.jpg'  WHERE recipe_id = 3;
UPDATE recipe SET image_url = '/images/recipe/seed-4.jpg'  WHERE recipe_id = 4;
UPDATE recipe SET image_url = '/images/recipe/seed-5.jpg'  WHERE recipe_id = 5;
UPDATE recipe SET image_url = '/images/recipe/seed-6.webp' WHERE recipe_id = 6;
UPDATE recipe SET image_url = '/images/recipe/seed-7.jpg'  WHERE recipe_id = 7;
UPDATE recipe SET image_url = '/images/recipe/seed-8.jpg'  WHERE recipe_id = 8;
UPDATE recipe SET image_url = '/images/recipe/seed-9.png'  WHERE recipe_id = 9;
UPDATE recipe SET image_url = '/images/recipe/seed-10.jpg' WHERE recipe_id = 10;
