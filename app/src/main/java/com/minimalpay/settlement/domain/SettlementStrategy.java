package com.minimalpay.settlement.domain;

import java.util.List;
import java.util.Map;

/**
 * GRASP Protected Variations: isolates split algorithm changes behind Strategy.
 */
public interface SettlementStrategy {
    Map<String, Long> calculateShares(long totalAmountMinor, Member payer, List<Member> participants);

    String getDisplayName();
}
