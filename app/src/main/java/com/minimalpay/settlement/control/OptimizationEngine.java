package com.minimalpay.settlement.control;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GRASP — Pure Fabrication: 최소 송금 경로 알고리즘.
 */
public class OptimizationEngine {

    public static final class TransferEdge {
        public final String fromMemberId;
        public final String toMemberId;
        public final long amountMinor;

        public TransferEdge(String fromMemberId, String toMemberId, long amountMinor) {
            this.fromMemberId = fromMemberId;
            this.toMemberId = toMemberId;
            this.amountMinor = amountMinor;
        }
    }

    public List<TransferEdge> computeMinimumTransfers(Map<String, Long> netBalanceMinor) {
        List<String> ids = new ArrayList<>(netBalanceMinor.keySet());
        Map<String, Integer> indexOf = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            indexOf.put(ids.get(i), i);
        }

        List<long[]> debtors = new ArrayList<>();
        List<long[]> creditors = new ArrayList<>();
        for (Map.Entry<String, Long> e : netBalanceMinor.entrySet()) {
            long v = e.getValue();
            if (v < 0) {
                debtors.add(new long[]{indexOf.get(e.getKey()), -v});
            } else if (v > 0) {
                creditors.add(new long[]{indexOf.get(e.getKey()), v});
            }
        }

        List<TransferEdge> result = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < debtors.size() && j < creditors.size()) {
            long pay = Math.min(debtors.get(i)[1], creditors.get(j)[1]);
            if (pay > 0) {
                result.add(new TransferEdge(
                        ids.get((int) debtors.get(i)[0]),
                        ids.get((int) creditors.get(j)[0]),
                        pay));
            }
            debtors.get(i)[1] -= pay;
            creditors.get(j)[1] -= pay;
            if (debtors.get(i)[1] == 0) {
                i++;
            }
            if (creditors.get(j)[1] == 0) {
                j++;
            }
        }
        return result;
    }
}
