package com.minimalpay.settlement.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 1/N split. Remainder won is assigned from the first participant onward.
 */
public class EqualSplitStrategy implements SettlementStrategy {

    @Override
    public Map<String, Long> calculateShares(long totalAmountMinor, Member payer, List<Member> participants) {
        if (participants == null || participants.isEmpty()) {
            throw new IllegalArgumentException("정산 참여 멤버가 필요합니다.");
        }
        int n = participants.size();
        long base = totalAmountMinor / n;
        long remainder = totalAmountMinor % n;

        Map<String, Long> shares = new LinkedHashMap<>();
        for (int i = 0; i < participants.size(); i++) {
            Member member = participants.get(i);
            long share = base + (i < remainder ? 1 : 0);
            shares.put(member.getId(), share);
        }
        SplitValidation.requireExactTotal(totalAmountMinor, shares);
        return shares;
    }

    @Override
    public String getDisplayName() {
        return "1/N 균등 정산";
    }
}
