package com.minimalpay.settlement.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GRASP — Information Expert: 1/N 균등 분할. 잔돈은 결제자(Payer)에게 귀속.
 */
public class EqualSplitStrategy implements SettlementStrategy {

    @Override
    public Map<String, Long> calculateShares(long totalAmountMinor, Member payer, List<Member> participants) {
        if (participants == null || participants.isEmpty()) {
            throw new IllegalArgumentException("참여 멤버가 필요합니다.");
        }
        int n = participants.size();
        long base = totalAmountMinor / n;
        long remainder = totalAmountMinor % n;

        Map<String, Long> shares = new LinkedHashMap<>();
        for (Member m : participants) {
            long share = base;
            if (payer != null && m.getId().equals(payer.getId())) {
                share += remainder;
            }
            shares.put(m.getId(), share);
        }

        long sum = 0;
        for (long v : shares.values()) {
            sum += v;
        }
        if (sum != totalAmountMinor) {
            throw new IllegalStateException("분할 합이 총액과 일치하지 않습니다.");
        }
        return shares;
    }
}
