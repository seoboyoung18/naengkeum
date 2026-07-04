package com.fridgefamer.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UnitConverterTest {

    private static void assertConv(String amount, String from, String to, String expected) {
        BigDecimal r = UnitConverter.convert(new BigDecimal(amount), from, to);
        assertEquals(0, new BigDecimal(expected).compareTo(r),
                () -> amount + from + " → " + to + " = " + r);
    }

    @Test
    @DisplayName("동일 단위는 그대로")
    void sameUnit() {
        assertConv("200", "g", "g", "200");
        assertConv("2", "개", "개", "2");
    }

    @Test
    @DisplayName("무게 변환 — kg↔g")
    void weight() {
        assertConv("1.5", "kg", "g", "1500");
        assertConv("500", "g", "kg", "0.5");
    }

    @Test
    @DisplayName("부피 변환 — L↔ml (대소문자 무관)")
    void volume() {
        assertConv("1", "L", "ml", "1000");
        assertConv("1", "l", "ml", "1000");
        assertConv("250", "ml", "L", "0.25");
    }

    @Test
    @DisplayName("차원이 다르면 변환 불가 — 개↔g, 큰술↔g")
    void incompatibleDimensions() {
        assertNull(UnitConverter.convert(new BigDecimal("2"), "개", "g"));
        assertNull(UnitConverter.convert(new BigDecimal("2"), "큰술", "g"));
        assertNull(UnitConverter.convert(new BigDecimal("2"), "공기", "g"));
    }
}
