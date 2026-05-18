package com.minimalpay.settlement.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GRASP — Information Expert: 그룹·멤버·지출 집합 관리.
 */
public class SettlementGroup {
    private final String id;
    private String name;
    private final Map<String, Member> members = new LinkedHashMap<>();
    private final List<Expense> expenses = new ArrayList<>();

    public SettlementGroup(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Collection<Member> getMembers() {
        return Collections.unmodifiableCollection(members.values());
    }

    public List<Expense> getExpenses() {
        return Collections.unmodifiableList(expenses);
    }

    public Member addMember(String name) {
        Member m = new Member("M-" + (members.size() + 1), name);
        members.put(m.getId(), m);
        return m;
    }

    public Member findMemberById(String id) {
        return members.get(id);
    }

    public void addExpense(Expense expense) {
        expenses.add(expense);
    }

    public void recalculateBalances() {
        for (Member m : members.values()) {
            m.addBalanceMinor(-m.getBalanceMinor());
        }
        for (Expense e : expenses) {
            e.getPayer().addBalanceMinor(e.getTotalAmountMinor());
            for (Member p : e.getParticipants()) {
                p.addBalanceMinor(-e.getShareMinor(p));
            }
        }
    }
}
