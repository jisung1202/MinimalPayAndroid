package com.minimalpay.settlement.control;

import java.util.Arrays;
import java.util.List;

/**
 * GRASP Creator / Pure Fabrication: exposes split mode choices to the UI.
 */
public final class StrategyFactory {
    public static final String KEY_EQUAL_SPLIT = "EQUAL_SPLIT";
    public static final String KEY_ITEMIZED_SPLIT = "ITEMIZED_SPLIT";
    public static final String KEY_FIXED_AMOUNT_SPLIT = "FIXED_AMOUNT_SPLIT";

    private StrategyFactory() {
    }

    public static List<StrategyOption> availableOptions() {
        return Arrays.asList(
                new StrategyOption(KEY_EQUAL_SPLIT, "1/N 균등 정산"),
                new StrategyOption(KEY_ITEMIZED_SPLIT, "개별 품목 정산"),
                new StrategyOption(KEY_FIXED_AMOUNT_SPLIT, "고정 금액 차감 정산")
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
