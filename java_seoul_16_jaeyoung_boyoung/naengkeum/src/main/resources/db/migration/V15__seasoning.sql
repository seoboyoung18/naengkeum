-- =====================================================================
--  V15__seasoning.sql
--  조미료(양념) 보유 관리 — "무게/유통기한 없이 보유 여부만 체크"
--
--  목적: 소금·간장 등 조미료는 수량 관리가 무의미하므로 fridge_item과 분리해
--        '마스터 목록(seasoning) + 회원 보유(member_seasoning)' 2테이블로 관리.
--        화면에서는 보유한 조미료를 토글(다중선택)로 체크한다.
--        조미료별 권장 보관 위치(냉장/냉동/실온)를 함께 제공해 분류가 가능하다.
--
--  설계:
--    - seasoning        : 대표 조미료 큐레이션 마스터(시드). 부족분은 후속 시드로 추가.
--    - member_seasoning : 회원별 보유 조인. 행이 존재하면 '보유'로 간주.
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
--  1. 조미료 마스터 (큐레이션)
-- ---------------------------------------------------------------------
CREATE TABLE seasoning (
    seasoning_id  BIGINT       NOT NULL AUTO_INCREMENT,
    name          VARCHAR(30)  NOT NULL                COMMENT '조미료명',
    sort_order    INT          NOT NULL DEFAULT 0      COMMENT '표시 순서(작을수록 앞)',
    storage_type  ENUM('FRIDGE','FREEZER','ROOM_TEMP')
                               NOT NULL DEFAULT 'ROOM_TEMP'
                                                       COMMENT '권장 보관 위치(냉장/냉동/실온 분류용)',
    storage_tip   VARCHAR(200) DEFAULT NULL            COMMENT '보관 팁(어디에 보관하면 좋은지)',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (seasoning_id),
    -- [방어] 조미료명 중복 방지(목록 정확도)
    UNIQUE KEY uq_seasoning_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='조미료 마스터(큐레이션)';

-- ---------------------------------------------------------------------
--  2. 회원 보유 조미료 (행 존재 = 보유)
-- ---------------------------------------------------------------------
CREATE TABLE member_seasoning (
    member_seasoning_id BIGINT   NOT NULL AUTO_INCREMENT,
    member_id           BIGINT   NOT NULL,
    seasoning_id        BIGINT   NOT NULL,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (member_seasoning_id),
    -- [방어] 동일 조미료 중복 보유행 방지
    UNIQUE KEY uq_member_seasoning (member_id, seasoning_id),
    CONSTRAINT fk_ms_member
        FOREIGN KEY (member_id)    REFERENCES member(member_id)       ON DELETE CASCADE,
    CONSTRAINT fk_ms_seasoning
        FOREIGN KEY (seasoning_id) REFERENCES seasoning(seasoning_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='회원 보유 조미료';

-- ---------------------------------------------------------------------
--  3. 대표 조미료 시드 (sort_order 10단위로 그룹 정렬)
--     기본양념 / 장류 / 기름류 / 소스류 / 가루·기타 / 단맛·전분 / 감칠맛
-- ---------------------------------------------------------------------
INSERT INTO seasoning (name, sort_order, storage_type, storage_tip) VALUES
  ('소금',     10, 'ROOM_TEMP', '습기 차단하여 밀폐 보관.'),
  ('설탕',     11, 'ROOM_TEMP', '습기 차단하여 밀폐 보관.'),
  ('후추',     12, 'ROOM_TEMP', '밀폐하여 서늘한 곳에 보관.'),
  ('식초',     13, 'ROOM_TEMP', '직사광선 피해 서늘한 곳에 보관.'),
  ('간장',     20, 'ROOM_TEMP', '개봉 후 냉장하면 풍미가 오래 유지됨.'),
  ('된장',     21, 'FRIDGE',    '개봉 후 냉장. 표면 마름 방지로 밀폐.'),
  ('고추장',   22, 'FRIDGE',    '개봉 후 냉장 보관.'),
  ('쌈장',     23, 'FRIDGE',    '개봉 후 냉장 보관.'),
  ('식용유',   30, 'ROOM_TEMP', '직사광선·열 피해 서늘한 곳. 산패 주의.'),
  ('올리브유', 31, 'ROOM_TEMP', '직사광선 피해 서늘한 곳에 보관.'),
  ('참기름',   32, 'ROOM_TEMP', '직사광선 피해 보관. 냉장 시 굳을 수 있음.'),
  ('들기름',   33, 'FRIDGE',    '산패가 빠르므로 냉장 보관 권장.'),
  ('굴소스',   40, 'FRIDGE',    '개봉 후 냉장 보관.'),
  ('마요네즈', 41, 'FRIDGE',    '개봉 후 냉장 필수.'),
  ('케첩',     42, 'FRIDGE',    '개봉 후 냉장 보관.'),
  ('액젓',     43, 'ROOM_TEMP', '직사광선 피해 보관. 개봉 후 냉장도 좋음.'),
  ('머스타드', 44, 'FRIDGE',    '개봉 후 냉장 보관.'),
  ('스리라차', 45, 'FRIDGE',    '개봉 후 냉장 보관.'),
  ('고춧가루', 50, 'FREEZER',   '냉동 보관이 색·향 유지에 좋음.'),
  ('다진마늘', 51, 'FREEZER',   '소분하여 냉동 보관.'),
  ('맛술',     52, 'ROOM_TEMP', '직사광선 피해 보관.'),
  ('물엿',     60, 'ROOM_TEMP', '실온 보관. 굳으면 중탕으로 녹임.'),
  ('올리고당', 61, 'ROOM_TEMP', '실온 보관.'),
  ('전분',     62, 'ROOM_TEMP', '습기 차단하여 밀폐 보관.'),
  ('밀가루',   63, 'ROOM_TEMP', '밀폐하여 서늘한 곳. 벌레 주의.'),
  ('통깨',     64, 'ROOM_TEMP', '밀폐 보관. 냉장·냉동도 가능.'),
  ('다시다',   65, 'ROOM_TEMP', '개봉 후 밀폐하여 서늘한 곳에 보관.'),
  ('미원',     66, 'ROOM_TEMP', '습기 차단하여 밀폐 보관.');
