-- =====================================================================
--  V10__report.sql
--  콘텐츠 신고(report) 모델 도입 — 레시피/리뷰 신고 + 관리자 모니터링
--
--  목적: 사용자가 부적절한 레시피/리뷰를 신고하고, 관리자가 신고 누적
--        콘텐츠를 우선 확인해 삭제할 수 있도록 한다.
--        (V8 시점엔 "신고 없이 관리자가 직접 삭제" 합의였으나, 2026-06-16
--         신고 기능을 추가하기로 변경 — 관리자 페이지에서 신고수 노출용)
--
--  설계 노트:
--    - wishlist(테이블 8)와 동일한 XOR 패턴: recipe_id / review_id 중
--      정확히 하나만 채워짐. target_type 컬럼 없이 어느 쪽이 NULL인지로 구분.
--    - reporter_id + 대상 UNIQUE: 한 회원이 같은 콘텐츠를 중복 신고 차단(→409).
--    - 모든 FK ON DELETE CASCADE: 신고 대상(레시피/리뷰)이나 신고자가
--      삭제되면 신고도 함께 정리. 관리자가 콘텐츠 삭제 시 신고가 자동 제거됨.
--    - status: 관리자 처리 상태. 현재는 전부 집계하지만, 추후 "처리완료"로
--      목록에서 내릴 수 있도록 컬럼만 미리 둔다(나중에 추가하기 번거로운 컬럼).
-- =====================================================================

CREATE TABLE report (
    report_id    BIGINT        NOT NULL AUTO_INCREMENT,
    reporter_id  BIGINT        NOT NULL                       COMMENT '신고한 회원',
    recipe_id    BIGINT        DEFAULT NULL                   COMMENT '신고 대상 레시피',
    review_id    BIGINT        DEFAULT NULL                   COMMENT '신고 대상 리뷰',
    reason       VARCHAR(255)  DEFAULT NULL                   COMMENT '신고 사유(선택)',
    status       ENUM('PENDING','RESOLVED')
                               NOT NULL DEFAULT 'PENDING'     COMMENT '처리 상태',
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (report_id),
    -- [방어] 한 회원이 같은 콘텐츠를 두 번 신고하는 것 차단 (NULL은 UNIQUE 우회 → 정상)
    UNIQUE KEY uq_report_recipe (reporter_id, recipe_id),
    UNIQUE KEY uq_report_review (reporter_id, review_id),
    CONSTRAINT fk_report_reporter
        FOREIGN KEY (reporter_id) REFERENCES member(member_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_report_recipe
        FOREIGN KEY (recipe_id) REFERENCES recipe(recipe_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_report_review
        FOREIGN KEY (review_id) REFERENCES review(review_id)
        ON DELETE CASCADE,
    -- [방어] recipe_id / review_id 중 정확히 하나만 채워져야 함 (XOR)
    CONSTRAINT chk_report_xor CHECK (
        (recipe_id IS NOT NULL AND review_id IS NULL) OR
        (recipe_id IS NULL     AND review_id IS NOT NULL)
    ),
    -- 관리자 목록 "신고 누적순" 정렬/집계용 인덱스
    KEY idx_report_recipe (recipe_id),
    KEY idx_report_review (review_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='콘텐츠 신고';
