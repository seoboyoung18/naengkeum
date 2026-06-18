-- =====================================================================
--  V13__member_social_login.sql
--  소셜 로그인(구글 / 카카오) 도입
--
--  목적: 기존 이메일+비밀번호(LOCAL) 로그인에 더해 OAuth2 소셜 로그인을
--        지원. 소셜 가입 회원은 비밀번호가 없고(NULL), 카카오는 이메일을
--        제공하지 않을 수 있어 이메일도 NULL을 허용한다.
--
--  식별 규칙:
--    - (social_provider, social_id) 조합으로 소셜 회원을 유일하게 식별.
--    - 같은 이메일의 기존 LOCAL 회원이 있으면 그 계정에 소셜을 연결(link).
--
--  새 컬럼 (member):
--    - social_provider : 가입 경로. 'LOCAL'(기본) / 'GOOGLE' / 'KAKAO'.
--    - social_id       : 소셜 제공자의 고유 사용자 ID (LOCAL이면 NULL).
--
--  컬럼 변경:
--    - password : NOT NULL → NULL 허용 (소셜 회원은 비밀번호 없음).
--    - email    : NOT NULL → NULL 허용 (카카오 이메일 미동의 가능).
--
--  ⚠️ 기존 uq_member_email(UNIQUE) 유지. MySQL은 UNIQUE 인덱스에서 NULL을
--     서로 다른 값으로 취급하므로 이메일 없는 소셜 회원이 여러 명이어도 무방.
-- =====================================================================

-- ---------------------------------------------------------------------
--  1. password / email NULL 허용으로 변경
-- ---------------------------------------------------------------------
ALTER TABLE member
    MODIFY COLUMN password VARCHAR(255) NULL
        COMMENT 'BCrypt 해시 (소셜 로그인 회원은 NULL)';

ALTER TABLE member
    MODIFY COLUMN email VARCHAR(100) NULL
        COMMENT '로그인 이메일 (카카오 등 소셜 회원은 미제공 가능 → NULL)';

-- ---------------------------------------------------------------------
--  2. 소셜 식별 컬럼 추가 (role 뒤에 배치)
-- ---------------------------------------------------------------------
ALTER TABLE member
    ADD COLUMN social_provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL'
        COMMENT '가입 경로 (LOCAL / GOOGLE / KAKAO)' AFTER role,
    ADD COLUMN social_id VARCHAR(100) DEFAULT NULL
        COMMENT '소셜 제공자의 고유 사용자 ID (LOCAL이면 NULL)' AFTER social_provider;

-- ---------------------------------------------------------------------
--  3. (provider, social_id) 유일 제약 + 조회 인덱스
--     LOCAL 회원은 social_id가 NULL이라 서로 충돌하지 않음.
-- ---------------------------------------------------------------------
ALTER TABLE member
    ADD UNIQUE KEY uq_member_social (social_provider, social_id);
