-- =====================================================================
--  냉큼(Naeng-Keum) — V2__indexes.sql
--  인덱스 전략 (ERD 설계서 "3. 인덱스 전략" 표 + 추가 보강)
--
--  원칙
--   1. PK / UNIQUE는 자동 인덱스이므로 여기서 추가하지 않음
--   2. FK 컬럼은 MySQL InnoDB가 자동으로 인덱스 생성 → 중복 방지
--      (단, 다중컬럼 인덱스 첫 번째 컬럼이 FK면 그게 FK 인덱스 역할 대신함)
--   3. WHERE/ORDER BY/JOIN에서 자주 쓰는 컬럼만 추가
--      → 잘못 추가하면 INSERT/UPDATE 비용만 증가
-- =====================================================================

-- ---------------------------------------------------------------------
-- FRIDGE_ITEM
--   - 가장 빈번한 쿼리: "내 냉장고 재료를 유통기한 임박순으로"
--   - idx_fridge_member는 FK 자동인덱스로 대체되므로 생략
--   - (member_id, expiry_date) 복합인덱스가 임박순 정렬의 핵심
-- ---------------------------------------------------------------------
CREATE INDEX idx_fridge_expiry
    ON fridge_item (member_id, expiry_date);

-- ---------------------------------------------------------------------
-- RECIPE
--   - 제목 검색: FULLTEXT 인덱스 (LIKE '%키워드%'보다 압도적으로 빠름)
--   - 조리 시간 필터: cook_time 단독 인덱스
--   - 인기순 정렬: view_count 인덱스 (F05 인기순 정렬용)
-- ---------------------------------------------------------------------
CREATE FULLTEXT INDEX idx_recipe_title
    ON recipe (title) WITH PARSER ngram;

CREATE INDEX idx_recipe_cook_time
    ON recipe (cook_time);

CREATE INDEX idx_recipe_view_count
    ON recipe (view_count DESC);

-- ---------------------------------------------------------------------
-- RECIPE_INGREDIENT
--   - "감자, 양파 가진 사람에게 추천 가능한 레시피" 매칭(F17 fridge/match)에서
--     name으로 IN 검색이 들어가므로 인덱스 필수
--   - recipe_id는 FK 자동인덱스로 충분
-- ---------------------------------------------------------------------
CREATE INDEX idx_ingredient_name
    ON recipe_ingredient (name);

-- ---------------------------------------------------------------------
-- REVIEW
--   - "이 레시피의 리뷰 목록" 조회 → FK 자동인덱스로 충분
--   - "내가 쓴 리뷰" 조회는 member_id FK 자동인덱스로 충분
--   - 단, recipe_id + created_at 정렬이 잦으므로 보조 인덱스 추가
-- ---------------------------------------------------------------------
CREATE INDEX idx_review_recipe_created
    ON review (recipe_id, created_at DESC);

-- ---------------------------------------------------------------------
-- WISHLIST
--   - "내 찜 목록 최신순" 조회 패턴
--   - member_id 단독은 FK 인덱스로 충분하나, created_at 정렬 가속 위해 복합 추가
-- ---------------------------------------------------------------------
CREATE INDEX idx_wishlist_member_created
    ON wishlist (member_id, created_at DESC);

-- AI 레시피 찜 조회용 (FK 인덱스 별도 필요)
CREATE INDEX idx_wishlist_ai_recipe
    ON wishlist (ai_recipe_id);

-- ---------------------------------------------------------------------
-- FOLLOW
--   - "나를 팔로우한 사람들" 조회 시 followee_id 단독 인덱스 필요
--     (FK 자동인덱스는 UNIQUE 복합키 첫 컬럼인 follower_id쪽에만 적용됨)
-- ---------------------------------------------------------------------
CREATE INDEX idx_follow_followee
    ON follow (followee_id);

-- ---------------------------------------------------------------------
-- CHALLENGE
--   - "현재 진행 중인 챌린지" = WHERE start_date <= NOW() <= end_date
-- ---------------------------------------------------------------------
CREATE INDEX idx_challenge_date
    ON challenge (start_date, end_date);

-- ---------------------------------------------------------------------
-- AI_RECIPE
--   - "내가 요청한 AI 레시피 히스토리" 조회 시 (member_id, created_at) 복합
-- ---------------------------------------------------------------------
CREATE INDEX idx_ai_recipe_member_created
    ON ai_recipe (member_id, created_at DESC);

-- =====================================================================
--  인덱스 적용 완료 (총 11개 추가 인덱스)
--  다음 실행 파일: V3__seed_data.sql
-- =====================================================================
