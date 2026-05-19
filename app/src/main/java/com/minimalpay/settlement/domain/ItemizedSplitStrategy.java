package com.minimalpay.settlement.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Splits each receipt item among a selected subset of members.
 */
public class ItemizedSplitStrategy implements SettlementStrategy {
    private final List<ItemShare> items;

    public ItemizedSplitStrategy(List<ItemShare> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("품목 정산에는 최소 1개 이상의 품목이 필요합니다.");
        }
        this.items = List.copyOf(items);
    }

    @Override
    public Map<String, Long> calculateShares(long totalAmountMinor, Member payer, List<Member> participants) {
        Map<String, Long> shares = new LinkedHashMap<>();
        for (Member member : participants) {
            shares.put(member.getId(), 0L);
        }

        long itemTotal = 0L;
        for (ItemShare item : items) {
            itemTotal += item.amountMinor;
            long base = item.amountMinor / item.memberIds.size();
            long remainder = item.amountMinor % item.memberIds.size();
            for (int i = 0; i < item.memberIds.size(); i++) {
                String memberId = item.memberIds.get(i);
                if (!shares.containsKey(memberId)) {
                    throw new IllegalArgumentException("품목 참여자가 전체 참여 멤버에 포함되어야 합니다: " + memberId);
                }
                long add = base + (i < remainder ? 1 : 0);
                shares.put(memberId, shares.get(memberId) + add);
            }
        }

        if (itemTotal != totalAmountMinor) {
            throw new IllegalArgumentException("품목 금액 합계가 전체 결제 금액과 일치해야 합니다.");
        }
        SplitValidation.requireExactTotal(totalAmountMinor, shares);
        return shares;
    }

    @Override
    public String getDisplayName() {
        return "개별 품목 정산";
    }

    public static final class ItemShare {
        public final String name;
        public final long amountMinor;
        public final List<String> memberIds;

        public ItemShare(String name, long amountMinor, List<String> memberIds) {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("품목 이름을 입력해 주세요.");
            }
            if (amountMinor <= 0) {
                throw new IllegalArgumentException("품목 금액은 1원 이상이어야 합니다.");
            }
            if (memberIds == null || memberIds.isEmpty()) {
                throw new IllegalArgumentException("품목별 참여자를 입력해 주세요.");
            }
            this.name = name.trim();
            this.amountMinor = amountMinor;
            this.memberIds = new ArrayList<>(memberIds);
        }
    }
}
