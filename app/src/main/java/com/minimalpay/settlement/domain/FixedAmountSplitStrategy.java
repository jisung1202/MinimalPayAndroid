package com.minimalpay.settlement.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Multiple members can pay fixed amounts; the remaining amount is split evenly.
 */
public class FixedAmountSplitStrategy implements SettlementStrategy {
    private final Map<String, Long> fixedAmountByMemberId;

    public FixedAmountSplitStrategy(Map<String, Long> fixedAmountByMemberId) {
        if (fixedAmountByMemberId == null || fixedAmountByMemberId.isEmpty()) {
            throw new IllegalArgumentException("고정 금액 대상자를 1명 이상 추가해 주세요.");
        }
        this.fixedAmountByMemberId = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : fixedAmountByMemberId.entrySet()) {
            if (entry.getKey() == null || entry.getKey().trim().isEmpty()) {
                throw new IllegalArgumentException("고정 금액 대상자를 선택해 주세요.");
            }
            if (entry.getValue() == null || entry.getValue() < 0) {
                throw new IllegalArgumentException("고정 금액은 0원 이상이어야 합니다.");
            }
            this.fixedAmountByMemberId.put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public Map<String, Long> calculateShares(long totalAmountMinor, Member payer, List<Member> participants) {
        Map<String, Long> shares = new LinkedHashMap<>();
        for (Member member : participants) {
            shares.put(member.getId(), 0L);
        }

        long fixedTotal = 0L;
        for (Map.Entry<String, Long> entry : fixedAmountByMemberId.entrySet()) {
            if (!shares.containsKey(entry.getKey())) {
                throw new IllegalArgumentException("고정 금액 대상자는 참여 멤버에 포함되어야 합니다.");
            }
            fixedTotal += entry.getValue();
            shares.put(entry.getKey(), entry.getValue());
        }
        if (fixedTotal > totalAmountMinor) {
            throw new IllegalArgumentException("고정 금액 합계가 전체 결제 금액보다 큽니다.");
        }

        long remaining = totalAmountMinor - fixedTotal;
        List<Member> variableMembers = participants.stream()
                .filter(member -> !fixedAmountByMemberId.containsKey(member.getId()))
                .toList();
        if (remaining > 0 && variableMembers.isEmpty()) {
            throw new IllegalArgumentException("나머지 금액을 나눌 멤버가 필요합니다.");
        }

        long base = variableMembers.isEmpty() ? 0 : remaining / variableMembers.size();
        long remainder = variableMembers.isEmpty() ? 0 : remaining % variableMembers.size();
        for (int i = 0; i < variableMembers.size(); i++) {
            Member member = variableMembers.get(i);
            shares.put(member.getId(), base + (i < remainder ? 1 : 0));
        }

        SplitValidation.requireExactTotal(totalAmountMinor, shares);
        return shares;
    }

    @Override
    public String getDisplayName() {
        return "고정 금액 차감 정산";
    }
}
