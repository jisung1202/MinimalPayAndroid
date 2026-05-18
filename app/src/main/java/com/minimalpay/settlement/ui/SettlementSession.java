package com.minimalpay.settlement.ui;

import com.minimalpay.settlement.control.SettlementController;
import com.minimalpay.settlement.domain.Member;

import java.util.ArrayList;
import java.util.List;

/** 앱 전역 상태: Controller + 멤버 + 마지막 리포트 */
public class SettlementSession {

    private final SettlementController controller = new SettlementController();
    private final List<Member> members = new ArrayList<>();
    private SettlementController.SettlementReport lastReport;
    private String groupName = "";
    private int expectedMemberCount;

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

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public void clearMembers() {
        members.clear();
    }

    public void addMember(Member member) {
        members.add(member);
    }

    public int getExpectedMemberCount() {
        return expectedMemberCount;
    }

    public void setExpectedMemberCount(int expectedMemberCount) {
        this.expectedMemberCount = expectedMemberCount;
    }

    public boolean isGroupStepComplete() {
        return getController().hasGroup()
                && expectedMemberCount > 0
                && members.size() >= expectedMemberCount;
    }
}
