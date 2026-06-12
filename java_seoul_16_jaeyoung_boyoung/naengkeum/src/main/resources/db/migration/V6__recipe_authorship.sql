-- =====================================================================
--  V6__recipe_authorship.sql
--  레시피 "작성자 + 공개여부" 모델 도입 (AI 레시피 등록 기능)
--
--  목적: AI가 추천한 레시피를 사용자가 "내 레시피로 담기"(개인 보관) 한 뒤,
--        검토 후 "공개하기"로 공개 카탈로그에 게시하는 흐름을 지원.
--        (생성=담기 와 공개=게시 를 분리 — 팀 합의 2026-06-10)
--
--  새 컬럼 (recipe):
--    - author_id           : 작성자 회원. NULL = 공공(식약처 시드) 레시피
--    - is_public           : 공개 여부. 공개 카탈로그(GET /api/recipe)는 TRUE만 노출
--    - source_ai_recipe_id : "담기"의 출처 ai_recipe (추적 + 중복 담기 방지)
--
--  데이터 정책:
--    - 기존 시드 레시피(V4)는 전부 is_public=TRUE 로 백필 (공개 카탈로그 유지)
--    - 신규 "담기" 레시피는 INSERT 시 is_public=FALSE (비공개 기본 — 실수로 즉시
--      공개되는 사고 방지). 컬럼 DEFAULT 도 FALSE.
--    - author_id ON DELETE SET NULL: 회원 탈퇴 시 레시피는 보존(공공처럼 전환).
--      → 공개된 레시피에 달린 리뷰/찜 등 커뮤니티 데이터 보호.
--    - source_ai_recipe_id ON DELETE SET NULL: ai_recipe가 정리돼도 복사본(스냅샷)은 유지.
-- =====================================================================

-- ---------------------------------------------------------------------
--  1. 컬럼 추가
-- ---------------------------------------------------------------------
ALTER TABLE recipe
    ADD COLUMN author_id           BIGINT  DEFAULT NULL
        COMMENT '작성자 회원 (NULL=공공 레시피)' AFTER source,
    ADD COLUMN is_public           BOOLEAN NOT NULL DEFAULT FALSE
        COMMENT '공개 여부 (TRUE만 공개 카탈로그 노출)' AFTER author_id,
    ADD COLUMN source_ai_recipe_id BIGINT  DEFAULT NULL
        COMMENT '담기 출처 ai_recipe (중복 담기 방지)' AFTER is_public;

-- ---------------------------------------------------------------------
--  2. 기존 시드(공공) 레시피 백필 → 전부 공개
-- ---------------------------------------------------------------------
UPDATE recipe SET is_public = TRUE;

-- ---------------------------------------------------------------------
--  3. 외래키
-- ---------------------------------------------------------------------
ALTER TABLE recipe
    ADD CONSTRAINT fk_recipe_author
        FOREIGN KEY (author_id) REFERENCES member(member_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_recipe_source_ai
        FOREIGN KEY (source_ai_recipe_id) REFERENCES ai_recipe(ai_recipe_id)
        ON DELETE SET NULL;

-- ---------------------------------------------------------------------
--  4. 인덱스 / 제약
--     - uq_recipe_author_srcai : 한 회원이 같은 ai_recipe를 두 번 담는 것 차단.
--       (author_id, source_ai_recipe_id 둘 다 NULL인 공공 레시피는 NULL 다중허용
--        규칙상 충돌하지 않음 — 여러 행 공존 가능)
--     - idx_recipe_public : 공개 카탈로그 필터(is_public) 조회용
-- ---------------------------------------------------------------------
ALTER TABLE recipe
    ADD UNIQUE KEY uq_recipe_author_srcai (author_id, source_ai_recipe_id),
    ADD KEY idx_recipe_public (is_public);
