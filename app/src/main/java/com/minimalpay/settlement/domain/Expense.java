package com.minimalpay.settlement.domain;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * GRASP — Information Expert / Creator: 지출·분할 결과 캡슐화. 분할은 Strategy에 위임.
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
        if (participants == null || participants.isEmpty()) {
            throw new IllegalArgumentException("참여 멤버가 필요합니다.");
        }
        if (strategy == null) {
            throw new IllegalArgumentException("정산 전략이 필요합니다.");
        }
        this.id = id;
        this.description = description;
        this.totalAmountMinor = totalAmountMinor;
        this.payer = payer;
        this.participants = List.copyOf(participants);
        this.strategyName = strategy.getClass().getSimpleName();
        this.shareByMemberIdMinor = strategy.calculateShares(totalAmountMinor, payer, participants);
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

    public long getShareMinor(Member m) {
        Long share = shareByMemberIdMinor.get(m.getId());
        return share != null ? share : 0L;
    }
}
