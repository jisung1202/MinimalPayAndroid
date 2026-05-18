package com.minimalpay.settlement.control;

import com.minimalpay.settlement.domain.EqualSplitStrategy;
import com.minimalpay.settlement.domain.SettlementStrategy;

import java.util.Arrays;
import java.util.List;

/**
 * GRASP — Creator / Pure Fabrication (Factory): UI 선택 → Strategy 인스턴스 생성.
 */
public final class StrategyFactory {
    public static final String KEY_EQUAL_SPLIT = "EQUAL_SPLIT";

    private StrategyFactory() {
    }

    public static SettlementStrategy create(String strategyKey) {
        if (KEY_EQUAL_SPLIT.equals(strategyKey)) {
            return new EqualSplitStrategy();
        }
        throw new IllegalArgumentException("지원하지 않는 정산 방식: " + strategyKey);
    }

    public static List<StrategyOption> availableOptions() {
        return Arrays.asList(
                new StrategyOption(KEY_EQUAL_SPLIT, "1/N 균등 (잔돈→결제자)")
        );
    }

    public static final class StrategyOption {
        public final String key;
        public final String label;

        public StrategyOption(String key, String label) {
            this.key = key;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
