package com.minimalpay.settlement.domain;

import java.util.List;
import java.util.Map;

/**
 * GRASP Information Expert / Creator: expense owns payment facts and delegates
 * share calculation to a settlement strategy.
 */
public class Expense {
    private final String id;
    private final String description;
    private final Member payer;
    private final List<Member> participants;
    private final long totalAmountMinor;
    private final String strategyName;
    private final Map<String, Long> shareByMemberIdMinor;

    public Expense(String id, String description, long totalAmountMinor,
                   Member payer, List<Member> participants, SettlementStrategy strategy) {
        if (totalAmountMinor <= 0) {
            throw new IllegalArgumentException("금액은 1원 이상이어야 합니다.");
        }
        if (payer == null) {
            throw new IllegalArgumentException("결제자를 선택해 주세요.");
        }
        if (participants == null || participants.isEmpty()) {
            throw new IllegalArgumentException("정산 참여 멤버가 필요합니다.");
        }
        if (strategy == null) {
            throw new IllegalArgumentException("정산 방식이 필요합니다.");
        }
        this.id = id;
        this.description = description;
        this.totalAmountMinor = totalAmountMinor;
        this.payer = payer;
        this.participants = List.copyOf(participants);
        this.strategyName = strategy.getDisplayName();
        this.shareByMemberIdMinor = strategy.calculateShares(totalAmountMinor, payer, participants);
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public Member getPayer() {
        return payer;
    }

    public List<Member> getParticipants() {
        return participants;
    }

    public long getTotalAmountMinor() {
        return totalAmountMinor;
    }

    public String getStrategyName() {
        return strategyName;
    }

    public long getShareMinor(Member member) {
        Long share = shareByMemberIdMinor.get(member.getId());
        return share != null ? share : 0L;
    }
}
