package com.minimalpay.settlement.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.minimalpay.settlement.MinimalPayApp;
import com.minimalpay.settlement.R;
import com.minimalpay.settlement.control.OptimizationEngine;
import com.minimalpay.settlement.control.SettlementController;
import com.minimalpay.settlement.control.TransferCallback;
import com.minimalpay.settlement.domain.Member;

import java.util.Map;

/**
 * UC-4: transfer linking extension.
 */
public class TransferFragment extends Fragment {

    private SettlementSession session;
    private TextView textPreview;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transfer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        session = MinimalPayApp.sessionFrom(requireActivity().getApplication());
        textPreview = view.findViewById(R.id.textTransferPreview);

        MaterialButton btnTransfer = view.findViewById(R.id.btnTransfer);
        btnTransfer.setOnClickListener(v -> onTransfer());
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePreview();
    }

    private void updatePreview() {
        SettlementController.SettlementReport report = session.getLastReport();
        if (report == null || report.getTransfers().isEmpty()) {
            textPreview.setText("먼저 [정산] 화면에서 리포트를 생성해 주세요.");
            return;
        }
        OptimizationEngine.TransferEdge edge = report.getTransfers().get(0);
        Map<String, Member> byId = new java.util.HashMap<>();
        for (Member member : session.getMembers()) {
            byId.put(member.getId(), member);
        }
        Member from = byId.get(edge.fromMemberId);
        Member to = byId.get(edge.toMemberId);
        if (from == null || to == null) {
            textPreview.setText("멤버 정보를 불러올 수 없습니다.");
            return;
        }
        String account = to.hasBankAccount() ? to.getBankAccount() : "계좌 미등록";
        textPreview.setText(String.format(
                "%s -> %s\n%,d원\n%s",
                from.getName(),
                to.getName(),
                edge.amountMinor,
                account));
    }

    private void onTransfer() {
        SettlementController.SettlementReport report = session.getLastReport();
        if (report == null || report.getTransfers().isEmpty()) {
            Toast.makeText(requireContext(), "먼저 정산 리포트를 생성해 주세요.", Toast.LENGTH_LONG).show();
            return;
        }
        OptimizationEngine.TransferEdge edge = report.getTransfers().get(0);
        session.getController().executeExternalTransfer(
                requireContext(),
                edge.fromMemberId,
                edge.toMemberId,
                edge.amountMinor,
                new TransferCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(requireContext(), "송금 처리를 완료했습니다.", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(String message) {
                        new AlertDialog.Builder(requireContext())
                                .setTitle("송금 실패")
                                .setMessage(message)
                                .setPositiveButton("확인", null)
                                .show();
                    }
                });
    }
}
