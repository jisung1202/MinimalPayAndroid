package com.minimalpay.settlement.ui;

import com.minimalpay.settlement.control.SettlementController;
import com.minimalpay.settlement.domain.Member;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** App-wide state shared by the four-step wizard. */
public class SettlementSession {

    private final SettlementController controller = new SettlementController();
    private final List<Member> members = new ArrayList<>();
    private SettlementController.SettlementReport lastReport;
    private String groupName = "";
    private int expectedMemberCount;
    private ExpenseDraft expenseDraft = new ExpenseDraft();

    public SettlementController getController() {
        return controller;
    }

    public List<Member> getMembers() {
        return members;
    }

    public SettlementController.SettlementReport getLastReport() {
        return lastReport;
    }

    public void setLastReport(SettlementController.SettlementReport lastReport) {
        this.lastReport = lastReport;
    }

    public ExpenseDraft getExpenseDraft() {
        return expenseDraft;
    }

    public void setExpenseDraft(ExpenseDraft expenseDraft) {
        this.expenseDraft = expenseDraft;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public void clearMembers() {
        members.clear();
        lastReport = null;
        expenseDraft = new ExpenseDraft();
    }

    public void addMember(Member member) {
        members.add(member);
        lastReport = null;
    }

    public int getExpectedMemberCount() {
        return expectedMemberCount;
    }

    public void setExpectedMemberCount(int expectedMemberCount) {
        this.expectedMemberCount = expectedMemberCount;
    }

    public void resetAll() {
        controller.reset();
        members.clear();
        lastReport = null;
        groupName = "";
        expectedMemberCount = 0;
        expenseDraft = new ExpenseDraft();
    }

    public boolean isGroupStepComplete() {
        return getController().hasGroup()
                && expectedMemberCount > 0
                && members.size() >= expectedMemberCount;
    }

    public static final class ExpenseDraft {
        public String description = "";
        public String amount = "";
        public String strategyKey = "";
        public String payerMemberId = "";
        public Set<String> participantIds = new HashSet<>();
        public List<FixedDraft> fixedRows = new ArrayList<>();
        public List<ItemDraft> itemRows = new ArrayList<>();
    }

    public static final class FixedDraft {
        public String memberId = "";
        public String amount = "";
    }

    public static final class ItemDraft {
        public String name = "";
        public String amount = "";
        public Set<String> participantIds = new HashSet<>();
    }
}
