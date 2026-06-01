-- =====================================================================
--  냉큼(Naeng-Keum) — V1__schema.sql
--  13개 테이블 스키마 정의 (MySQL 8 / InnoDB / utf8mb4)
--
--  작성 기준
--   - ERD 설계서 v1.0 + 요구사항 명세서 F01~F20
--   - 명명규칙: snake_case, PK는 {table}_id BIGINT AUTO_INCREMENT
--   - 시간 컬럼: created_at / updated_at DEFAULT CURRENT_TIMESTAMP
--
--  ⚠️ 테이블 생성 순서는 FK 의존성에 따라 결정됨:
--     member → fridge_item → recipe → recipe_ingredient / recipe_step
--     → review → ai_recipe → wishlist (ai_recipe FK 참조)
--     → follow → badge → challenge → challenge_participant → member_badge
--
--  주의: FK 의존성 순서대로 작성됨. 위에서 아래로 순차 실행 가능.
-- =====================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------
-- 0. 기존 테이블 제거 (개발 환경 재실행 대비)
--    운영 환경에서는 절대 사용 금지. Flyway/Liquibase로 관리할 것.
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS member_badge;
DROP TABLE IF EXISTS challenge_participant;
DROP TABLE IF EXISTS challenge;
DROP TABLE IF EXISTS badge;
DROP TABLE IF EXISTS follow;
DROP TABLE IF EXISTS wishlist;
DROP TABLE IF EXISTS ai_recipe;
DROP TABLE IF EXISTS review;
DROP TABLE IF EXISTS recipe_step;
DROP TABLE IF EXISTS recipe_ingredient;
DROP TABLE IF EXISTS recipe;
DROP TABLE IF EXISTS fridge_item;
DROP TABLE IF EXISTS member;

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================================
--  1. MEMBER — 회원
-- =====================================================================
CREATE TABLE member (
    member_id        BIGINT       NOT NULL AUTO_INCREMENT          COMMENT '회원 고유 ID',
    email            VARCHAR(100) NOT NULL                         COMMENT '로그인 이메일',
    password         VARCHAR(255) NOT NULL                         COMMENT 'BCrypt 해시 (최소 60자)',
    nickname         VARCHAR(20)  NOT NULL                         COMMENT '화면 표시명',
    allergies        VARCHAR(255) DEFAULT NULL                     COMMENT '알레르기 식재료 (콤마 구분)',
    marketing_agree  TINYINT(1)   NOT NULL DEFAULT 0               COMMENT '마케팅 수신 동의 (0/1)',
    is_active        TINYINT(1)   NOT NULL DEFAULT 1               COMMENT '소프트 삭제 플래그 (0=탈퇴)',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                                  ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (member_id),
    UNIQUE KEY uq_member_email    (email),
    UNIQUE KEY uq_member_nickname (nickname)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='회원 정보';

-- =====================================================================
--  2. FRIDGE_ITEM — 냉장고 재료
-- =====================================================================
CREATE TABLE fridge_item (
    fridge_item_id  BIGINT         NOT NULL AUTO_INCREMENT,
    member_id       BIGINT         NOT NULL                        COMMENT '소유 회원',
    name            VARCHAR(50)    NOT NULL                        COMMENT '재료명',
    qty             DECIMAL(10,2)  NOT NULL                        COMMENT '수량',
    unit            VARCHAR(10)    NOT NULL                        COMMENT '단위 (개/g/ml/L 등)',
    storage_type    ENUM('FRIDGE','FREEZER','ROOM_TEMP')
                                   NOT NULL DEFAULT 'FRIDGE'       COMMENT '보관 위치',
    expiry_date     DATE           NOT NULL                        COMMENT '유통기한일',
    memo            VARCHAR(100)   DEFAULT NULL                    COMMENT '사용자 메모',
    created_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP
                                   ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (fridge_item_id),
    CONSTRAINT fk_fridge_member
        FOREIGN KEY (member_id) REFERENCES member(member_id)
        ON DELETE CASCADE,
    -- [방어] 음수 수량 차단. LLM 추천에서 qty 0/음수가 들어오면 프롬프트가 망가짐
    CONSTRAINT chk_fridge_qty_positive CHECK (qty >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='냉장고 재료';

-- =====================================================================
--  3. RECIPE — 레시피 (공공 데이터 + 사용자 작성)
-- =====================================================================
CREATE TABLE recipe (
    recipe_id       BIGINT        NOT NULL AUTO_INCREMENT,
    title           VARCHAR(100)  NOT NULL                         COMMENT '레시피명',
    summary         TEXT          DEFAULT NULL                     COMMENT '간단 소개',
    cook_time       INT           DEFAULT NULL                     COMMENT '조리 시간 (분)',
    image_url       VARCHAR(500)  DEFAULT NULL,
    source          VARCHAR(50)   DEFAULT 'PUBLIC'                 COMMENT '출처 (PUBLIC/USER/AI_SAVED)',
    view_count      INT           NOT NULL DEFAULT 0,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
                                  ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (recipe_id),
    -- [방어] cook_time이 들어올 경우 음수/0 금지. NULL은 허용 (시간 미지정 레시피 존재)
    CONSTRAINT chk_recipe_cook_time CHECK (cook_time IS NULL OR cook_time > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='레시피 마스터';

-- =====================================================================
--  4. RECIPE_INGREDIENT — 레시피별 재료
-- =====================================================================
CREATE TABLE recipe_ingredient (
    ingredient_id   BIGINT        NOT NULL AUTO_INCREMENT,
    recipe_id       BIGINT        NOT NULL,
    name            VARCHAR(50)   NOT NULL                         COMMENT '재료명 (정규화 X, 원본 보존)',
    qty             VARCHAR(50)   DEFAULT NULL                     COMMENT '수량 표기 (예: "2개", "100g", "적당량")',
    PRIMARY KEY (ingredient_id),
    CONSTRAINT fk_ingredient_recipe
        FOREIGN KEY (recipe_id) REFERENCES recipe(recipe_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='레시피 재료 목록';

-- =====================================================================
--  5. RECIPE_STEP — 레시피 조리 순서
-- =====================================================================
CREATE TABLE recipe_step (
    step_id         BIGINT        NOT NULL AUTO_INCREMENT,
    recipe_id       BIGINT        NOT NULL,
    step_number     INT           NOT NULL                         COMMENT '순서 (1부터 시작)',
    description     TEXT          NOT NULL                         COMMENT '조리 설명',
    image_url       VARCHAR(500)  DEFAULT NULL,
    PRIMARY KEY (step_id),
    -- [방어] 같은 레시피 내 step_number 중복 방지
    UNIQUE KEY uq_recipe_step (recipe_id, step_number),
    CONSTRAINT fk_step_recipe
        FOREIGN KEY (recipe_id) REFERENCES recipe(recipe_id)
        ON DELETE CASCADE,
    CONSTRAINT chk_step_number_positive CHECK (step_number > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='레시피 조리 단계';

-- =====================================================================
--  6. REVIEW — 레시피 리뷰
-- =====================================================================
CREATE TABLE review (
    review_id       BIGINT        NOT NULL AUTO_INCREMENT,
    member_id       BIGINT        NOT NULL,
    recipe_id       BIGINT        NOT NULL,
    rating          INT           NOT NULL                         COMMENT '평점 1~5',
    content         TEXT          NOT NULL                         COMMENT '리뷰 본문',
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
                                  ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (review_id),
    -- [방어] 한 회원이 같은 레시피에 중복 리뷰 작성 차단 (409 응답 트리거)
    UNIQUE KEY uq_review_member_recipe (member_id, recipe_id),
    CONSTRAINT fk_review_member
        FOREIGN KEY (member_id) REFERENCES member(member_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_review_recipe
        FOREIGN KEY (recipe_id) REFERENCES recipe(recipe_id)
        ON DELETE CASCADE,
    -- [방어] 평점 1~5 범위 강제
    CONSTRAINT chk_review_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='레시피 리뷰';

-- =====================================================================
--  7. AI_RECIPE — AI 생성 레시피
--     ⚠️ wishlist가 이 테이블의 FK를 참조하므로 반드시 wishlist보다 먼저 생성
-- =====================================================================
CREATE TABLE ai_recipe (
    ai_recipe_id     BIGINT       NOT NULL AUTO_INCREMENT,
    member_id        BIGINT       NOT NULL                         COMMENT '요청한 회원',
    title            VARCHAR(100) NOT NULL                         COMMENT 'AI 생성 레시피명',
    summary          TEXT         DEFAULT NULL                     COMMENT 'AI 생성 소개',
    ingredients_json JSON         NOT NULL                         COMMENT '재료 JSON 배열',
    steps_json       JSON         NOT NULL                         COMMENT '조리 순서 JSON 배열',
    cook_time        INT          DEFAULT NULL                     COMMENT '예상 조리 시간 (분)',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (ai_recipe_id),
    CONSTRAINT fk_ai_recipe_member
        FOREIGN KEY (member_id) REFERENCES member(member_id)
        ON DELETE CASCADE,
    -- [방어] JSON 타입 추가 검증. MySQL 8.0.17+ CHECK 지원
    --        ingredients_json은 반드시 배열이어야 함 (LLM이 객체 반환 시 차단)
    CONSTRAINT chk_ai_ingredients_array  CHECK (JSON_TYPE(ingredients_json) = 'ARRAY'),
    CONSTRAINT chk_ai_steps_array        CHECK (JSON_TYPE(steps_json)       = 'ARRAY')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 생성 레시피';

-- =====================================================================
--  8. WISHLIST — 찜 (일반 레시피 + AI 레시피 모두 지원)
--
--  ⚠️ 설계 노트: recipe_id와 ai_recipe_id의 ON DELETE 정책에 대해
--   - 원래 ERD 설계서는 fk_wishlist_recipe를 ON DELETE SET NULL로 정의
--   - 그러나 MySQL 8은 CHECK 제약에 사용된 컬럼을 SET NULL action으로
--     변경할 수 없음 (Error 3823): 원본 레시피 삭제 시 recipe_id가
--     NULL이 되면서 ai_recipe_id도 NULL이면 XOR CHECK가 깨지기 때문
--   - 또한 ERD 원본에는 fk_wishlist_ai_recipe FK 자체가 누락되어 있어
--     존재하지 않는 AI 레시피 ID를 찜할 수 있는 데이터 무결성 구멍 존재
--
--  ✅ 결정: 양쪽 모두 ON DELETE CASCADE 적용
--     - 공공 레시피는 사실상 삭제될 일이 거의 없으나,
--       만약 삭제 시 죽은 링크를 마이페이지에 남기는 것보다
--       찜도 함께 삭제하는 것이 UX 측면에서 자연스러움
--     - AI 레시피도 동일 원칙 (요청 회원이 탈퇴해서 AI 레시피 삭제 시)
-- =====================================================================
CREATE TABLE wishlist (
    wishlist_id     BIGINT        NOT NULL AUTO_INCREMENT,
    member_id       BIGINT        NOT NULL,
    recipe_id       BIGINT        DEFAULT NULL                     COMMENT '일반 레시피 찜',
    ai_recipe_id    BIGINT        DEFAULT NULL                     COMMENT 'AI 생성 레시피 찜',
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (wishlist_id),
    -- [방어] 일반 레시피 중복 찜 방지 (NULL 값은 UNIQUE 제약을 우회하므로
    --        한 회원이 동일 recipe_id로 두 번 찜하는 케이스만 막아줌)
    UNIQUE KEY uq_wishlist_member_recipe (member_id, recipe_id),
    UNIQUE KEY uq_wishlist_member_ai     (member_id, ai_recipe_id),
    CONSTRAINT fk_wishlist_member
        FOREIGN KEY (member_id) REFERENCES member(member_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_wishlist_recipe
        FOREIGN KEY (recipe_id) REFERENCES recipe(recipe_id)
        ON DELETE CASCADE,
    -- [신규 추가] ERD 원본에 누락되어 있던 AI 레시피 FK
    CONSTRAINT fk_wishlist_ai_recipe
        FOREIGN KEY (ai_recipe_id) REFERENCES ai_recipe(ai_recipe_id)
        ON DELETE CASCADE,
    -- [방어] recipe_id와 ai_recipe_id 중 정확히 하나만 채워져야 함 (XOR)
    --        CASCADE 정책 덕분에 CHECK 제약과 충돌 없음
    CONSTRAINT chk_wishlist_xor CHECK (
        (recipe_id IS NOT NULL AND ai_recipe_id IS NULL) OR
        (recipe_id IS NULL     AND ai_recipe_id IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='찜 목록';

-- =====================================================================
--  9. FOLLOW — 팔로우 관계
-- =====================================================================
CREATE TABLE follow (
    follow_id       BIGINT        NOT NULL AUTO_INCREMENT,
    follower_id     BIGINT        NOT NULL                         COMMENT '팔로우 거는 쪽',
    followee_id     BIGINT        NOT NULL                         COMMENT '팔로우 당하는 쪽',
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (follow_id),
    UNIQUE KEY uq_follow (follower_id, followee_id),
    CONSTRAINT fk_follow_follower
        FOREIGN KEY (follower_id) REFERENCES member(member_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_follow_followee
        FOREIGN KEY (followee_id) REFERENCES member(member_id)
        ON DELETE CASCADE,
    -- [방어] 자기 자신을 팔로우하는 케이스 차단
    CONSTRAINT chk_follow_self CHECK (follower_id <> followee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='팔로우 관계';

-- =====================================================================
--  10. BADGE — 배지 마스터
--      CHALLENGE가 badge_id를 FK로 참조하므로 먼저 생성해야 함
-- =====================================================================
CREATE TABLE badge (
    badge_id        BIGINT        NOT NULL AUTO_INCREMENT,
    name            VARCHAR(50)   NOT NULL                         COMMENT '배지명',
    description     VARCHAR(255)  DEFAULT NULL                     COMMENT '달성 조건 설명',
    icon_url        VARCHAR(500)  DEFAULT NULL                     COMMENT '배지 이미지 URL',
    PRIMARY KEY (badge_id),
    UNIQUE KEY uq_badge_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='배지 마스터';

-- =====================================================================
--  11. CHALLENGE — 챌린지
-- =====================================================================
CREATE TABLE challenge (
    challenge_id    BIGINT        NOT NULL AUTO_INCREMENT,
    badge_id        BIGINT        NOT NULL                         COMMENT '달성 시 지급 배지',
    title           VARCHAR(100)  NOT NULL,
    description     TEXT          DEFAULT NULL,
    start_date      DATE          NOT NULL,
    end_date        DATE          NOT NULL,
    rules           TEXT          DEFAULT NULL                     COMMENT 'JSON 문자열 (조건 정의)',
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (challenge_id),
    CONSTRAINT fk_challenge_badge
        FOREIGN KEY (badge_id) REFERENCES badge(badge_id)
        ON DELETE RESTRICT,
    -- [방어] start_date가 end_date보다 늦는 잘못된 데이터 차단
    CONSTRAINT chk_challenge_dates CHECK (start_date <= end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='챌린지';

-- =====================================================================
--  12. CHALLENGE_PARTICIPANT — 챌린지 참여
-- =====================================================================
CREATE TABLE challenge_participant (
    participant_id  BIGINT        NOT NULL AUTO_INCREMENT,
    challenge_id    BIGINT        NOT NULL,
    member_id       BIGINT        NOT NULL,
    progress        INT           NOT NULL DEFAULT 0               COMMENT '진행률 0~100',
    is_achieved     TINYINT(1)    NOT NULL DEFAULT 0,
    joined_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    achieved_at     DATETIME      DEFAULT NULL,
    PRIMARY KEY (participant_id),
    -- [방어] 같은 챌린지 중복 참여 차단 (409 응답 트리거)
    UNIQUE KEY uq_participant (challenge_id, member_id),
    CONSTRAINT fk_participant_challenge
        FOREIGN KEY (challenge_id) REFERENCES challenge(challenge_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_participant_member
        FOREIGN KEY (member_id) REFERENCES member(member_id)
        ON DELETE CASCADE,
    CONSTRAINT chk_progress_range CHECK (progress BETWEEN 0 AND 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='챌린지 참여';

-- =====================================================================
--  13. MEMBER_BADGE — 회원 보유 배지
-- =====================================================================
CREATE TABLE member_badge (
    member_badge_id BIGINT        NOT NULL AUTO_INCREMENT,
    member_id       BIGINT        NOT NULL,
    badge_id        BIGINT        NOT NULL,
    earned_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (member_badge_id),
    -- [방어] 동일 배지 중복 지급 차단
    UNIQUE KEY uq_member_badge (member_id, badge_id),
    CONSTRAINT fk_member_badge_member
        FOREIGN KEY (member_id) REFERENCES member(member_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_member_badge_badge
        FOREIGN KEY (badge_id) REFERENCES badge(badge_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='회원 획득 배지';

-- =====================================================================
--  스키마 생성 완료 (총 13개 테이블)
--  다음 실행 파일: V2__indexes.sql
-- =====================================================================