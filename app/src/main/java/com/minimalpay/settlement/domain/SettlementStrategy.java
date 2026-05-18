package com.minimalpay.settlement.domain;

import java.util.List;
import java.util.Map;

/**
 * GRASP — Protected Variations: 정산 알고리즘 변화를 인터페이스로 격리 (Strategy Pattern).
 */
public interface SettlementStrategy {
    Map<String, Long> calculateShares(long totalAmountMinor, Member payer, List<Member> participants);
}
