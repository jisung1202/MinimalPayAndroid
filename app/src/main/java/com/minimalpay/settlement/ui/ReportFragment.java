package com.minimalpay.settlement.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.minimalpay.settlement.MinimalPayApp;
import com.minimalpay.settlement.R;
import com.minimalpay.settlement.control.OptimizationEngine;
import com.minimalpay.settlement.control.SettlementController;
import com.minimalpay.settlement.domain.Member;

import java.util.HashMap;
import java.util.Map;

/**
 * UC-3: requestSettlementReport().
 */
public class ReportFragment extends Fragment {

    private SettlementSession session;
    private TextView textReportEmpty;
    private TextView textReportGroup;
    private TextView textBalanceTitle;
    private TextView textTransferTitle;
    private LinearLayout containerBalances;
    private LinearLayout containerTransfers;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_report, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        session = MinimalPayApp.sessionFrom(requireActivity().getApplication());
        textReportEmpty = view.findViewById(R.id.textReportEmpty);
        textReportGroup = view.findViewById(R.id.textReportGroup);
        textBalanceTitle = view.findViewById(R.id.textBalanceTitle);
        textTransferTitle = view.findViewById(R.id.textTransferTitle);
        containerBalances = view.findViewById(R.id.containerBalances);
        containerTransfers = view.findViewById(R.id.containerTransfers);

        MaterialButton btnReport = view.findViewById(R.id.btnReport);
        btnReport.setOnClickListener(v -> onRequestReport());
        renderReport(session.getLastReport());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (session != null) {
            renderReport(session.getLastReport());
        }
    }

    private void onRequestReport() {
        try {
            SettlementController.SettlementReport report =
                    session.getController().requestSettlementReport();
            session.setLastReport(report);
            renderReport(report);
            Toast.makeText(requireContext(), "정산 리포트를 생성했습니다.", Toast.LENGTH_SHORT).show();
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).refreshWizardState();
            }
        } catch (Exception ex) {
            Toast.makeText(requireContext(), ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void renderReport(@Nullable SettlementController.SettlementReport report) {
        containerBalances.removeAllViews();
        containerTransfers.removeAllViews();

        boolean hasReport = report != null;
        textReportEmpty.setVisibility(hasReport ? View.GONE : View.VISIBLE);
        textReportGroup.setVisibility(hasReport ? View.VISIBLE : View.GONE);
        textBalanceTitle.setVisibility(hasReport ? View.VISIBLE : View.GONE);
        textTransferTitle.setVisibility(hasReport ? View.VISIBLE : View.GONE);
        if (!hasReport) {
            return;
        }

        textReportGroup.setText("그룹: " + report.getGroupName());
        Map<String, Member> membersById = new HashMap<>();
        for (Member member : report.getMembers()) {
            membersById.put(member.getId(), member);
        }

        for (Map.Entry<String, Long> entry : report.getNetBalanceMinor().entrySet()) {
            Member member = membersById.get(entry.getKey());
            if (member == null) {
                continue;
            }
            long amount = entry.getValue();
            String role = amount > 0 ? "받을 금액" : (amount < 0 ? "보낼 금액" : "정산 완료");
            containerBalances.addView(createInfoRow(
                    member.getName(),
                    String.format("%+,d원", amount),
                    role));
        }

        if (report.getTransfers().isEmpty()) {
            containerTransfers.addView(createInfoRow("송금 없음", "0원", "모두 정산되었습니다."));
        } else {
            for (OptimizationEngine.TransferEdge transfer : report.getTransfers()) {
                Member from = membersById.get(transfer.fromMemberId);
                Member to = membersById.get(transfer.toMemberId);
                if (from == null || to == null) {
                    continue;
                }
                String account = to.hasBankAccount() ? to.getBankAccount() : "계좌 미등록";
                containerTransfers.addView(createInfoRow(
                        from.getName() + " -> " + to.getName(),
                        String.format("%,d원", transfer.amountMinor),
                        account));
            }
        }
    }

    private View createInfoRow(String title, String amount, String subtitle) {
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setGravity(android.view.Gravity.CENTER_VERTICAL);
        root.setBackgroundResource(R.drawable.bg_card);
        root.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams rootParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rootParams.bottomMargin = dp(8);
        root.setLayoutParams(rootParams);

        LinearLayout textGroup = new LinearLayout(requireContext());
        textGroup.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        root.addView(textGroup, textParams);

        TextView titleView = new TextView(requireContext());
        titleView.setText(title);
        titleView.setTextColor(getResources().getColor(R.color.mp_text_primary, null));
        titleView.setTextSize(15);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        textGroup.addView(titleView);

        TextView subtitleView = new TextView(requireContext());
        subtitleView.setText(subtitle);
        subtitleView.setTextColor(getResources().getColor(R.color.mp_text_secondary, null));
        subtitleView.setTextSize(12);
        textGroup.addView(subtitleView);

        TextView amountView = new TextView(requireContext());
        amountView.setText(amount);
        amountView.setTextColor(getResources().getColor(R.color.mp_primary, null));
        amountView.setTextSize(15);
        amountView.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(amountView);
        return root;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
