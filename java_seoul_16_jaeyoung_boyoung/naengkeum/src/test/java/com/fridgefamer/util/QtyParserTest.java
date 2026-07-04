package com.fridgefamer.util;

import com.fridgefamer.util.QtyParser.ParsedQty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * QtyParser 단위 테스트 — Spring 컨텍스트/DB 불필요(순수 함수).
 * 실제 AI 레시피 재료 샘플(돼지고기 200g / 양파 0.5개 / 고춧가루 2큰술 등) 기반.
 */
class QtyParserTest {

    private static void assertQty(String raw, String amount, String unit) {
        ParsedQty p = QtyParser.parse(raw);
        assertEquals(0, new BigDecimal(amount).compareTo(p.amount()),
                () -> "수량 불일치: " + raw + " → " + p.amount());
        assertEquals(unit, p.unit(), () -> "단위 불일치: " + raw);
    }

    @Test
    @DisplayName("g 단위 정수 — 돼지고기 200g")
    void parsesGrams() {
        assertQty("200g", "200", "g");
    }

    @Test
    @DisplayName("개 단위 소수 — 양파 0.5개")
    void parsesDecimalCount() {
        assertQty("0.5개", "0.5", "개");
    }

    @Test
    @DisplayName("개 단위 정수 — 상추 4개 / 대파 1개")
    void parsesIntegerCount() {
        assertQty("4개", "4", "개");
        assertQty("1개", "1", "개");
    }

    @Test
    @DisplayName("조미료성 단위도 일단 파싱은 됨(제외는 상위 조미료 카탈로그가 담당)")
    void parsesSpoonAndOthers() {
        assertQty("2큰술", "2", "큰술");
        assertQty("0.5작은술", "0.5", "작은술");
        assertQty("1쪽", "1", "쪽");      // 마늘
        assertQty("2공기", "2", "공기");   // 쌀밥
    }

    @Test
    @DisplayName("kg/ml/L — 단위 표기 그대로 보존(변환은 상위 단계에서)")
    void preservesWeightVolumeUnits() {
        assertQty("1.5kg", "1.5", "kg");
        assertQty("300ml", "300", "ml");
        assertQty("1L", "1", "L");
    }

    @Test
    @DisplayName("분수 표기 — 1/4단 → 0.25 단")
    void parsesFraction() {
        assertQty("1/4단", "0.25", "단");
        assertQty("1/2개", "0.5", "개");
    }

    @Test
    @DisplayName("숫자와 단위 사이 공백 허용 — 200 g")
    void allowsSpaceBetween() {
        assertQty("200 g", "200", "g");
    }

    @Test
    @DisplayName("비정량 표기는 null — 적당량/약간/빈값/단위없는숫자")
    void rejectsNonQuantitative() {
        assertNull(QtyParser.parse("적당량"));
        assertNull(QtyParser.parse("약간"));
        assertNull(QtyParser.parse("조금"));
        assertNull(QtyParser.parse(""));
        assertNull(QtyParser.parse("   "));
        assertNull(QtyParser.parse(null));
        assertNull(QtyParser.parse("200"), "단위 없는 숫자는 냉장고와 매칭 불가 → 스킵");
        assertNull(QtyParser.parse("0개"), "수량 0은 차감 의미 없음 → 스킵");
    }
}
