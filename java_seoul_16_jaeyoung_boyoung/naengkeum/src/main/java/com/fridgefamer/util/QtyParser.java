package com.fridgefamer.util;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 레시피 재료의 수량 표기(자유 텍스트)를 (수량, 단위)로 분리하는 순수 파서.
 *
 * <p>레시피 재료의 qty는 {@code "200g"}, {@code "0.5개"}, {@code "2큰술"},
 * {@code "적당량"}처럼 정량/비정량이 섞인 자유 텍스트다(recipe_ingredient.qty VARCHAR).
 * 냉장고 차감은 정량(숫자+단위)만 대상으로 하므로, 여기서 정량만 추출하고
 * 비정량("적당량"·"약간" 등 선두 숫자가 없는 표기)은 {@code null}로 떨군다.</p>
 *
 * <p>이 클래스는 외부 의존성이 없는 순수 함수다. 단위 호환 판정(개↔g 불가,
 * g↔kg 변환 등)과 조미료 제외, 실제 차감은 상위 서비스가 담당한다 —
 * 여기서는 "문자열을 숫자+단위로 나누는 것"까지만 책임진다.</p>
 */
public final class QtyParser {

    private QtyParser() {}

    /**
     * 선두의 수량(분수/소수/정수) + 나머지(단위)로 분리. 선두 숫자가 없으면 매칭 실패.
     * 분수("1/4")를 먼저 시도해야 한다 — 정수 패턴을 앞에 두면 "1/4"가 "1"+"/4"로 쪼개진다.
     */
    private static final Pattern QTY = Pattern.compile(
            "^\\s*(\\d+\\s*/\\s*\\d+|\\d+(?:\\.\\d+)?)\\s*(.*)$");

    /**
     * 파싱 결과. 수량은 0보다 큰 값, 단위는 트림된 표기("g"·"개"·"큰술" 등).
     * 냉장고 단위와의 호환 판정은 호출 측에서 수행한다.
     */
    public record ParsedQty(BigDecimal amount, String unit) {}

    /**
     * 수량 문자열을 (수량, 단위)로 파싱한다.
     *
     * @param raw 레시피 재료 수량 표기(예: "200g", "0.5개", "1/4단", "적당량", null)
     * @return 정량이면 {@link ParsedQty}, 비정량(선두 숫자 없음)·빈 단위·수량 0이하·null이면 {@code null}
     */
    public static ParsedQty parse(String raw) {
        if (raw == null) return null;
        Matcher m = QTY.matcher(raw.trim());
        if (!m.matches()) return null;          // "적당량", "약간" 등 → 차감 대상 아님

        BigDecimal amount = toAmount(m.group(1));
        if (amount == null || amount.signum() <= 0) return null;

        String unit = m.group(2).trim();
        if (unit.isEmpty()) return null;        // 단위 없는 "200" 등은 냉장고와 매칭 불가 → 스킵

        return new ParsedQty(amount, unit);
    }

    /** "0.5"·"200"·"1/4" → BigDecimal. 분모 0이거나 형식 이상이면 null. */
    private static BigDecimal toAmount(String num) {
        try {
            int slash = num.indexOf('/');
            if (slash < 0) {
                return new BigDecimal(num);
            }
            BigDecimal numerator = new BigDecimal(num.substring(0, slash).trim());
            BigDecimal denominator = new BigDecimal(num.substring(slash + 1).trim());
            if (denominator.signum() == 0) return null;
            // 분수는 소수 4자리까지 반올림(1/3 같은 무한소수 방어)
            return numerator.divide(denominator, 4, java.math.RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
