-- =====================================================================
--  V7__recipe_nutrition_backfill.sql
--  영양정보 누락 시드 레시피 10건 백필
--
--  배경: V4 공공레시피 175건 중 10건이 영양정보(calories/carbs/protein/fat/sodium)가
--        NULL이라 상세 화면에서 영양 섹션이 숨겨졌다. 발표/데모 시 일부 레시피만
--        영양정보가 비어 보이는 것을 막기 위해, 잘 알려진 요리별 1인분 추정치로 채운다.
--
--  값 출처: 일반적인 식품 영양 가이드 기반 1인분 추정치(참고용). 단위: kcal / g / g / g / mg.
--  안전장치: calories IS NULL 인 행만 갱신(이미 값이 있는 165건은 건드리지 않음).
-- =====================================================================

UPDATE recipe SET calories=200, carbs=3.0,  protein=13.0, fat=14.0, sodium=320  WHERE title='계란말이'           AND calories IS NULL;
UPDATE recipe SET calories=480, carbs=70.0, protein=12.0, fat=16.0, sodium=900  WHERE title='김치볶음밥'         AND calories IS NULL;
UPDATE recipe SET calories=180, carbs=12.0, protein=11.0, fat=9.0,  sodium=1100 WHERE title='된장찌개'           AND calories IS NULL;
UPDATE recipe SET calories=160, carbs=24.0, protein=3.0,  fat=6.0,  sodium=380  WHERE title='감자볶음'           AND calories IS NULL;
UPDATE recipe SET calories=230, carbs=2.0,  protein=38.0, fat=7.0,  sodium=420  WHERE title='닭가슴살 스테이크'  AND calories IS NULL;
UPDATE recipe SET calories=320, carbs=3.0,  protein=19.0, fat=25.0, sodium=520  WHERE title='치즈 오믈렛'        AND calories IS NULL;
UPDATE recipe SET calories=520, carbs=72.0, protein=13.0, fat=19.0, sodium=600  WHERE title='파스타 알리오올리오' AND calories IS NULL;
UPDATE recipe SET calories=90,  carbs=6.0,  protein=7.0,  fat=4.0,  sodium=700  WHERE title='미역국'             AND calories IS NULL;
UPDATE recipe SET calories=560, carbs=85.0, protein=16.0, fat=16.0, sodium=850  WHERE title='비빔밥'             AND calories IS NULL;
UPDATE recipe SET calories=420, carbs=88.0, protein=8.0,  fat=6.0,  sodium=1000 WHERE title='떡볶이'             AND calories IS NULL;
