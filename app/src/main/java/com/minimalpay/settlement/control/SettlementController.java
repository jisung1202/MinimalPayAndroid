package com.minimalpay.settlement.control;

import android.content.Context;

import com.minimalpay.settlement.domain.EqualSplitStrategy;
import com.minimalpay.settlement.domain.Expense;
import com.minimalpay.settlement.domain.Member;
import com.minimalpay.settlement.domain.SettlementGroup;
import com.minimalpay.settlement.domain.SettlementStrategy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GRASP Controller: entry point for settlement use cases.
 */
public class SettlementController {
    private SettlementGroup currentGroup;
    private final OptimizationEngine optimizationEngine = new OptimizationEngine();
    private final TransferLinker transferLinker = new ManualAccountTransferLinker();

    public void createGroup(String groupName) {
        currentGroup = new SettlementGroup("G-1", groupName);
    }

    public void reset() {
        currentGroup = null;
    }

    public Member addMember(String memberName, String optionalAccount) {
        requireGroup();
        Member member = currentGroup.addMember(memberName);
        if (optionalAccount != null && !optionalAccount.trim().isEmpty()) {
            member.setBankAccount(optionalAccount);
        }
        return member;
    }

    public Collection<Member> getMembers() {
        requireGroup();
        return currentGroup.getMembers();
    }

    public boolean hasGroup() {
        return currentGroup != null;
    }

    public int getExpenseCount() {
        return currentGroup != null ? currentGroup.getExpenses().size() : 0;
    }

    public List<Expense> getExpenses() {
        requireGroup();
        return currentGroup.getExpenses();
    }

    public Expense registerExpense(String description, long amountWon, String payerMemberId,
                                   List<String> participantMemberIds) {
        return registerExpense(description, amountWon, payerMemberId, participantMemberIds,
                new EqualSplitStrategy());
    }

    public Expense registerExpense(String description, long amountWon, String payerMemberId,
                                   List<String> participantMemberIds, SettlementStrategy strategy) {
        requireGroup();
        Member payer = currentGroup.findMemberById(payerMemberId);
        if (payer == null) {
            throw new IllegalArgumentException("결제자를 찾을 수 없습니다.");
        }

        List<Member> participants = new ArrayList<>();
        for (String id : participantMemberIds) {
            Member member = currentGroup.findMemberById(id);
            if (member != null && !participants.contains(member)) {
                participants.add(member);
            }
        }
        if (participants.isEmpty()) {
            throw new IllegalArgumentException("참여 멤버를 선택해 주세요.");
        }

        Expense expense = new Expense(
                "E-" + (currentGroup.getExpenses().size() + 1),
                description,
                amountWon,
                payer,
                participants,
                strategy);
        currentGroup.addExpense(expense);
        currentGroup.recalculateBalances();
        return expense;
    }

    /**
     * SSD Operation for UC-3.
     */
    public SettlementReport requestSettlementReport() {
        requireGroup();
        currentGroup.recalculateBalances();
        Map<String, Long> net = new LinkedHashMap<>();
        for (Member member : currentGroup.getMembers()) {
            net.put(member.getId(), member.getBalanceMinor());
        }
        return new SettlementReport(
                currentGroup.getName(),
                net,
                optimizationEngine.computeMinimumTransfers(net),
                currentGroup.getMembers());
    }

    /** UC-4 external transfer extension. */
    public void executeExternalTransfer(Context context, String fromMemberId, String toMemberId,
                                        long amountWon, TransferCallback callback) {
        requireGroup();
        Member from = currentGroup.findMemberById(fromMemberId);
        Member to = currentGroup.findMemberById(toMemberId);
        if (from == null || to == null) {
            callback.onFailure("멤버를 찾을 수 없습니다.");
            return;
        }
        transferLinker.openTransfer(context, from, to, amountWon, callback);
    }

    private void requireGroup() {
        if (currentGroup == null) {
            throw new IllegalStateException("먼저 그룹을 생성해 주세요.");
        }
    }

    public static final class SettlementReport {
        private final String groupName;
        private final Map<String, Long> netBalanceMinor;
        private final List<OptimizationEngine.TransferEdge> transfers;
        private final Collection<Member> members;

        public SettlementReport(String groupName, Map<String, Long> netBalanceMinor,
                                List<OptimizationEngine.TransferEdge> transfers,
                                Collection<Member> members) {
            this.groupName = groupName;
            this.netBalanceMinor = netBalanceMinor;
            this.transfers = transfers;
            this.members = members;
        }

        public String getGroupName() {
            return groupName;
        }

        public Map<String, Long> getNetBalanceMinor() {
            return netBalanceMinor;
        }

        public List<OptimizationEngine.TransferEdge> getTransfers() {
            return transfers;
        }

        public Collection<Member> getMembers() {
            return members;
        }

        public String formatAsText() {
            Map<String, Member> byId = new HashMap<>();
            for (Member member : members) {
                byId.put(member.getId(), member);
            }

            StringBuilder sb = new StringBuilder();
            sb.append("MinimalPay 정산 리포트 - ").append(groupName).append("\n\n");
            sb.append("[개인별 잔액]\n");
            for (Map.Entry<String, Long> entry : netBalanceMinor.entrySet()) {
                Member member = byId.get(entry.getKey());
                long value = entry.getValue();
                String role = value > 0 ? "받을 금액" : (value < 0 ? "보낼 금액" : "정산 완료");
                sb.append(String.format("  %s : %+,d원 (%s)%n", member.getName(), value, role));
            }

            sb.append("\n[최소 송금 경로]\n");
            if (transfers.isEmpty()) {
                sb.append("  송금이 필요하지 않습니다.\n");
            } else {
                for (OptimizationEngine.TransferEdge transfer : transfers) {
                    Member from = byId.get(transfer.fromMemberId);
                    Member to = byId.get(transfer.toMemberId);
                    String account = to.hasBankAccount() ? to.getBankAccount() : "계좌 미등록";
                    sb.append(String.format(
                            "  %s -> %s : %,d원 (%s)%n",
                            from.getName(),
                            to.getName(),
                            transfer.amountMinor,
                            account));
                }
            }
            return sb.toString();
        }
    }
}
