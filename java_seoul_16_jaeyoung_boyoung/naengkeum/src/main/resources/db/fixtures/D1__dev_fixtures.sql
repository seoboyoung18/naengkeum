-- =====================================================================
--  냉큼(Naeng-Keum) — D1__dev_fixtures.sql
--  개발 환경 전용 가짜 데이터 (Dev Fixtures)
--
--  🚨 절대 운영 환경에서 실행 금지 🚨
--
--  목적
--   - 회원가입을 매번 하지 않아도 즉시 로그인/테스트 가능
--   - 냉장고 대시보드의 임박 알림(D-3) 시각화를 위한 다양한 D-day 분포
--   - 2주차 Auth/Member/Fridge API 개발 시 즉시 호출 가능한 더미 데이터
--
--  파일명 규칙
--   - V로 시작하면 Flyway가 운영 환경에도 적용 → 사고 위험
--   - D로 시작하면 Flyway 기본 명명규칙에서 무시됨 → 개발자가 수동 실행
--   - 또는 spring.flyway.locations를 환경별로 분리하여 dev에서만 로드
--
--  비밀번호 안내
--   - 모든 테스트 회원의 raw 비밀번호: Test1234!
--   - BCrypt 해시 (strength=10):
--     $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. MEMBER — 테스트 회원 2명
--    test1: 알레르기 있는 자취 페르소나
--    test2: 알레르기 없는 초보 페르소나 (빈 냉장고 케이스 테스트용)
-- ---------------------------------------------------------------------
INSERT INTO member (email, password, nickname, allergies, marketing_agree) VALUES
  ('test1@naengkeum.com',
   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
   '자취왕민지', '땅콩,새우', 1),
  ('test2@naengkeum.com',
   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
   '냉파초보', NULL, 0);

-- ---------------------------------------------------------------------
-- 2. FRIDGE_ITEM — test1 회원의 가짜 냉장고
--
--    의도적 D-day 분포 (대시보드 임박 알림 테스트):
--    D-1  : 우유                   ← 빨강 (긴급)
--    D-2  : 대파                   ← 빨강 (긴급)
--    D-3  : 계란                   ← 주황 (임박)
--    D-5  : 치즈                   ← 노랑 (주의)
--    D-7  : 양파                   ← 정상
--    D-14 : 감자                   ← 정상
--    D-30+: 김치, 닭가슴살, 쌀, 간장 ← 여유
--
--    storage_type도 골고루:
--    FRIDGE     : 양파/감자/계란/우유/치즈/대파/김치 (7)
--    FREEZER    : 닭가슴살 (1)
--    ROOM_TEMP  : 쌀/간장 (2)
--    → /api/fridge?storageType=FREEZER 필터 테스트 가능
-- ---------------------------------------------------------------------
INSERT INTO fridge_item (member_id, name, qty, unit, storage_type, expiry_date, memo) VALUES
  (1, '양파',       3,   '개',  'FRIDGE',   DATE_ADD(CURDATE(), INTERVAL  7 DAY), NULL),
  (1, '감자',       5,   '개',  'FRIDGE',   DATE_ADD(CURDATE(), INTERVAL 14 DAY), NULL),
  (1, '계란',      10,   '개',  'FRIDGE',   DATE_ADD(CURDATE(), INTERVAL  3 DAY), '대란'),
  (1, '우유',     900,   'ml',  'FRIDGE',   DATE_ADD(CURDATE(), INTERVAL  1 DAY), '개봉함'),
  (1, '치즈',     200,   'g',   'FRIDGE',   DATE_ADD(CURDATE(), INTERVAL  5 DAY), NULL),
  (1, '대파',       1,   '단',  'FRIDGE',   DATE_ADD(CURDATE(), INTERVAL  2 DAY), NULL),
  (1, '닭가슴살',  500,   'g',   'FREEZER',  DATE_ADD(CURDATE(), INTERVAL 60 DAY), NULL),
  (1, '쌀',     5000,   'g',   'ROOM_TEMP',DATE_ADD(CURDATE(), INTERVAL 90 DAY), NULL),
  (1, '간장',     500,   'ml',  'ROOM_TEMP',DATE_ADD(CURDATE(), INTERVAL 180 DAY), NULL),
  (1, '김치',     800,   'g',   'FRIDGE',   DATE_ADD(CURDATE(), INTERVAL 30 DAY), '묵은지');

-- test2는 일부러 빈 냉장고로 두기
-- → "재료 미등록 시 등록 유도 배너" Empty State UI 테스트용

-- =====================================================================
--  Dev Fixtures 적재 완료
--
--  확인 쿼리
--    SELECT member_id, email, nickname FROM member;
--    SELECT name, expiry_date,
--           DATEDIFF(expiry_date, CURDATE()) AS d_day
--      FROM fridge_item
--     WHERE member_id = 1
--     ORDER BY expiry_date ASC;
--
--  로그인 테스트 (2주차 Auth API 완성 후)
--    POST /api/auth/login
--    { "email": "test1@naengkeum.com", "password": "Test1234!" }
-- =====================================================================
