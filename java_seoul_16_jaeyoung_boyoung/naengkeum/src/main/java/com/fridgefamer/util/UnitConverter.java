package com.fridgefamer.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * 재료 수량 단위 변환 — 냉장고 차감 시 "레시피 단위 → 냉장고 단위" 환산.
 *
 * <p>차원(무게/부피/개수)이 같아야만 변환한다. {@code g↔kg}, {@code ml↔L}은 변환 가능,
 * {@code 개↔g}처럼 차원이 다르면 변환 불가({@code null})다. 냉장고 단위는 실무상
 * 개/g/ml이므로(스키마 주석 기준) 큰술·작은술·쪽·공기 같은 레시피 단위는
 * 냉장고 단위와 차원이 달라 자연히 변환 불가가 되어 차감 대상에서 빠진다.</p>
 *
 * <p>외부 의존성이 없는 순수 함수다.</p>
 */
public final class UnitConverter {

    private UnitConverter() {}

    private enum Dim { WEIGHT, VOLUME, COUNT }

    /** 단위 → (차원, 기준단위 환산계수). 기준: 무게=g, 부피=ml, 개수=개. */
    private record UnitDef(Dim dim, BigDecimal toBase) {}

    private static final Map<String, UnitDef> UNITS = Map.of(
            "g",  new UnitDef(Dim.WEIGHT, new BigDecimal("1")),
            "kg", new UnitDef(Dim.WEIGHT, new BigDecimal("1000")),
            "mg", new UnitDef(Dim.WEIGHT, new BigDecimal("0.001")),
            "ml", new UnitDef(Dim.VOLUME, new BigDecimal("1")),
            "cc", new UnitDef(Dim.VOLUME, new BigDecimal("1")),
            "l",  new UnitDef(Dim.VOLUME, new BigDecimal("1000")),
            "개", new UnitDef(Dim.COUNT,  new BigDecimal("1"))
    );

    /**
     * {@code amount}({@code from} 단위)를 {@code to} 단위 값으로 환산한다.
     *
     * @return 환산값. 단위 표기가 완전히 같으면 그대로. 차원이 다르거나
     *         변환표에 없는 단위면 {@code null}(=차감 불가).
     */
    public static BigDecimal convert(BigDecimal amount, String from, String to) {
        if (amount == null || from == null || to == null) return null;
        String f = norm(from);
        String t = norm(to);
        if (f.equals(t)) return amount;             // 동일 단위(쪽↔쪽 등)는 변환표 없이도 OK

        UnitDef uf = UNITS.get(f);
        UnitDef ut = UNITS.get(t);
        if (uf == null || ut == null || uf.dim() != ut.dim()) return null;  // 차원 불일치 → 불가

        // amount(from) → 기준단위 → to.  소수 4자리 반올림(0.5kg=500g 류는 정확, 무한소수만 방어).
        return amount.multiply(uf.toBase()).divide(ut.toBase(), 4, RoundingMode.HALF_UP);
    }

    /** 영문 단위는 소문자로 통일(L=l, ML=ml). 한글 단위(개 등)는 트림만. */
    private static String norm(String unit) {
        return unit.trim().toLowerCase();
    }
}
