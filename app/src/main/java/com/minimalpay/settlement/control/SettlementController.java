package com.minimalpay.settlement.control;

import android.content.Context;

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
 * GRASP — Controller: 시스템 이벤트 첫 진입점.
 */
public class SettlementController {
    private SettlementGroup currentGroup;
    private final OptimizationEngine optimizationEngine = new OptimizationEngine();
    private final TransferLinker transferLinker = new ManualAccountTransferLinker();

    public void createGroup(String groupName) {
        currentGroup = new SettlementGroup("G-1", groupName);
    }

    public Member addMember(String memberName, String optionalAccount) {
        requireGroup();
        Member m = currentGroup.addMember(memberName);
        if (optionalAccount != null && !optionalAccount.trim().isEmpty()) {
            m.setBankAccount(optionalAccount);
        }
        return m;
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

    public Expense registerExpense(String description, long amountWon, String payerMemberId,
                                   List<String> participantMemberIds, String settlementStrategyKey) {
        requireGroup();
        Member payer = currentGroup.findMemberById(payerMemberId);
        if (payer == null) {
            throw new IllegalArgumentException("결제자를 찾을 수 없습니다.");
        }

        List<Member> participants = new ArrayList<>();
        for (String id : participantMemberIds) {
            Member m = currentGroup.findMemberById(id);
            if (m != null && !participants.contains(m)) {
                participants.add(m);
            }
        }
        if (participants.isEmpty()) {
            throw new IllegalArgumentException("참여 멤버를 선택하세요.");
        }

        SettlementStrategy strategy = StrategyFactory.create(settlementStrategyKey);
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
     * SSD Operation — UC-3
     */
    public SettlementReport requestSettlementReport() {
        requireGroup();
        currentGroup.recalculateBalances();
        Map<String, Long> net = new LinkedHashMap<>();
        for (Member m : currentGroup.getMembers()) {
            net.put(m.getId(), m.getBalanceMinor());
        }
        return new SettlementReport(
                currentGroup.getName(),
                net,
                optimizationEngine.computeMinimumTransfers(net),
                currentGroup.getMembers());
    }

  /** UC-4 */
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
            throw new IllegalStateException("먼저 그룹을 생성하세요.");
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

        public List<OptimizationEngine.TransferEdge> getTransfers() {
            return transfers;
        }

        public String formatAsText() {
            Map<String, Member> byId = new HashMap<>();
            for (Member m : members) {
                byId.put(m.getId(), m);
            }
            StringBuilder sb = new StringBuilder();
            sb.append("══════════════════════════════════════\n");
            sb.append("  MinimalPay 정산 리포트: ").append(groupName).append("\n");
            sb.append("══════════════════════════════════════\n\n");
            sb.append("【 순잔액 】\n");
            for (Map.Entry<String, Long> e : netBalanceMinor.entrySet()) {
                Member m = byId.get(e.getKey());
                String role = e.getValue() > 0 ? "받을 돈" : (e.getValue() < 0 ? "줄 돈" : "완료");
                sb.append(String.format("  • %s : %+,d원 (%s)%n", m.getName(), e.getValue(), role));
            }
            sb.append("\n【 최소 송금 경로 】\n");
            if (transfers.isEmpty()) {
                sb.append("  (송금이 필요하지 않습니다)\n");
            } else {
                for (OptimizationEngine.TransferEdge t : transfers) {
                    Member from = byId.get(t.fromMemberId);
                    Member to = byId.get(t.toMemberId);
                    String account = to.hasBankAccount() ? to.getBankAccount() : "계좌 미등록";
                    sb.append(String.format(
                            "  ▶ [%s] ──(%s원)──> [%s] (%s)%n",
                            from.getName(),
                            String.format("%,d", t.amountMinor),
                            to.getName(),
                            account));
                }
            }
            sb.append("\n──────────────────────────────────────\n");
            return sb.toString();
        }
    }
}
