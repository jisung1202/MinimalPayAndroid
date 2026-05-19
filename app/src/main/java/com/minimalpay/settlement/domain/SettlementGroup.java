package com.minimalpay.settlement.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GRASP Information Expert: manages group members, expenses, and balances.
 */
public class SettlementGroup {
    private final String id;
    private final String name;
    private final Map<String, Member> members = new LinkedHashMap<>();
    private final List<Expense> expenses = new ArrayList<>();

    public SettlementGroup(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
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
        Member member = new Member("M-" + (members.size() + 1), name);
        members.put(member.getId(), member);
        return member;
    }

    public Member findMemberById(String id) {
        return members.get(id);
    }

    public void addExpense(Expense expense) {
        expenses.add(expense);
    }

    public void recalculateBalances() {
        for (Member member : members.values()) {
            member.addBalanceMinor(-member.getBalanceMinor());
        }
        for (Expense expense : expenses) {
            expense.getPayer().addBalanceMinor(expense.getTotalAmountMinor());
            for (Member participant : expense.getParticipants()) {
                participant.addBalanceMinor(-expense.getShareMinor(participant));
            }
        }
    }
}
