-- =====================================================================
--  V11__member_profile_image.sql
--  회원 프로필 사진 도입
--
--  목적: 마이페이지/프로필의 아바타를 닉네임 첫 글자(기본) 대신 사용자가
--        올린 사진으로 표시. 사진을 올리지 않으면 NULL → 프론트는 기존
--        글자 아바타로 폴백(기존 동작 유지).
--
--  저장 방식: 레시피 사진과 동일하게 로컬 디스크(app.upload.dir) + /images 정적 서빙.
--             컬럼에는 공개 URL(예: /images/profile/{uuid}.jpg)만 저장.
-- =====================================================================

ALTER TABLE member
    ADD COLUMN profile_image_url VARCHAR(500) DEFAULT NULL
        COMMENT '프로필 사진 공개 URL (NULL이면 기본 글자 아바타)' AFTER nickname;
