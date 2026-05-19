package com.minimalpay.settlement.domain;

import java.util.Map;

final class SplitValidation {
    private SplitValidation() {
    }

    static void requireExactTotal(long totalAmountMinor, Map<String, Long> shares) {
        long sum = 0L;
        for (long value : shares.values()) {
            if (value < 0) {
                throw new IllegalArgumentException("개인 부담액은 음수가 될 수 없습니다.");
            }
            sum += value;
        }
        if (sum != totalAmountMinor) {
            throw new IllegalStateException("정산 합계가 결제 금액과 일치하지 않습니다.");
        }
    }
}
