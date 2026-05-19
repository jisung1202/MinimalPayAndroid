package com.minimalpay.settlement.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One member pays a fixed amount, the remaining amount is split evenly.
 */
public class FixedAmountSplitStrategy implements SettlementStrategy {
    private final String fixedMemberId;
    private final long fixedAmountMinor;

    public FixedAmountSplitStrategy(String fixedMemberId, long fixedAmountMinor) {
        if (fixedMemberId == null || fixedMemberId.trim().isEmpty()) {
            throw new IllegalArgumentException("고정 금액 대상자를 선택해 주세요.");
        }
        if (fixedAmountMinor < 0) {
            throw new IllegalArgumentException("고정 금액은 0원 이상이어야 합니다.");
        }
        this.fixedMemberId = fixedMemberId;
        this.fixedAmountMinor = fixedAmountMinor;
    }

    @Override
    public Map<String, Long> calculateShares(long totalAmountMinor, Member payer, List<Member> participants) {
        if (fixedAmountMinor > totalAmountMinor) {
            throw new IllegalArgumentException("고정 금액이 전체 결제 금액보다 큽니다.");
        }
        Map<String, Long> shares = new LinkedHashMap<>();
        List<Member> variableMembers = participants.stream()
                .filter(member -> !member.getId().equals(fixedMemberId))
                .toList();
        if (variableMembers.isEmpty() && fixedAmountMinor != totalAmountMinor) {
            throw new IllegalArgumentException("나머지 금액을 나눌 멤버가 필요합니다.");
        }

        long remaining = totalAmountMinor - fixedAmountMinor;
        long base = variableMembers.isEmpty() ? 0 : remaining / variableMembers.size();
        long remainder = variableMembers.isEmpty() ? 0 : remaining % variableMembers.size();
        int variableIndex = 0;

        for (Member member : participants) {
            if (member.getId().equals(fixedMemberId)) {
                shares.put(member.getId(), fixedAmountMinor);
            } else {
                shares.put(member.getId(), base + (variableIndex < remainder ? 1 : 0));
                variableIndex++;
            }
        }
        if (!shares.containsKey(fixedMemberId)) {
            throw new IllegalArgumentException("고정 금액 대상자가 참여 멤버에 포함되어야 합니다.");
        }
        SplitValidation.requireExactTotal(totalAmountMinor, shares);
        return shares;
    }

    @Override
    public String getDisplayName() {
        return "고정 금액 차감 정산";
    }
}
