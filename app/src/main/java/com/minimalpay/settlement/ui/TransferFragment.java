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
import com.minimalpay.settlement.control.TransferDeepLinkBuilder;
import com.minimalpay.settlement.domain.Member;

import java.util.HashMap;
import java.util.Map;

public class TransferFragment extends Fragment {

    private SettlementSession session;
    private View panelTransferReady;
    private TextView textTransferEmpty;
    private TextView textTransferFrom;
    private TextView textTransferTo;
    private TextView textTransferAmount;
    private TextView textTransferAccount;
    private TextView textTransferLink;
    private MaterialButton btnTransfer;

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
        panelTransferReady = view.findViewById(R.id.panelTransferReady);
        textTransferEmpty = view.findViewById(R.id.textTransferEmpty);
        textTransferFrom = view.findViewById(R.id.textTransferFrom);
        textTransferTo = view.findViewById(R.id.textTransferTo);
        textTransferAmount = view.findViewById(R.id.textTransferAmount);
        textTransferAccount = view.findViewById(R.id.textTransferAccount);
        textTransferLink = view.findViewById(R.id.textTransferLink);
        btnTransfer = view.findViewById(R.id.btnTransfer);
        btnTransfer.setOnClickListener(v -> onTransfer());
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePreview();
    }

    private void updatePreview() {
        SettlementController.SettlementReport report = session.getLastReport();
        boolean hasTransfer = report != null && !report.getTransfers().isEmpty();
        panelTransferReady.setVisibility(hasTransfer ? View.VISIBLE : View.GONE);
        textTransferEmpty.setVisibility(hasTransfer ? View.GONE : View.VISIBLE);
        btnTransfer.setEnabled(hasTransfer);
        btnTransfer.setAlpha(hasTransfer ? 1f : 0.5f);
        if (!hasTransfer) {
            textTransferEmpty.setText("정산 리포트를 생성하면 송금 정보가 표시됩니다. 딥링크를 만들지 않아도 하단 [완료]로 마칠 수 있습니다.");
            return;
        }

        OptimizationEngine.TransferEdge edge = report.getTransfers().get(0);
        Map<String, Member> byId = new HashMap<>();
        for (Member member : session.getMembers()) {
            byId.put(member.getId(), member);
        }
        Member from = byId.get(edge.fromMemberId);
        Member to = byId.get(edge.toMemberId);
        if (from == null || to == null) {
            textTransferEmpty.setVisibility(View.VISIBLE);
            panelTransferReady.setVisibility(View.GONE);
            textTransferEmpty.setText("멤버 정보를 불러올 수 없습니다. 하단 [완료]로 수동 종료할 수 있습니다.");
            btnTransfer.setEnabled(false);
            btnTransfer.setAlpha(0.5f);
            return;
        }

        textTransferFrom.setText(from.getName());
        textTransferTo.setText(to.getName());
        textTransferAmount.setText(String.format("%,d원", edge.amountMinor));
        textTransferAccount.setText(to.hasBankAccount() ? to.getBankAccount() : "계좌 미등록 · 송금 시 직접 입력");
        if (to.hasBankAccount()) {
            String link = TransferDeepLinkBuilder.build(from, to, edge.amountMinor).toString();
            textTransferLink.setText("생성된 딥링크를 정산해야 하는 인원에게 전송하세요.\n" + link);
        } else {
            textTransferLink.setText("송금 실행 시 계좌 입력 후 딥링크가 생성됩니다. 딥링크를 만들지 않아도 하단 [완료]로 마칠 수 있습니다.");
        }
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
                        Toast.makeText(requireContext(), "송금 딥링크 처리를 완료했습니다.", Toast.LENGTH_SHORT).show();
                        updatePreview();
                    }

                    @Override
                    public void onFailure(String message) {
                        new AlertDialog.Builder(requireContext())
                                .setTitle("송금 실패")
                                .setMessage(message + "\n\n딥링크 없이 진행하려면 하단 [완료]를 눌러 마칠 수 있습니다.")
                                .setPositiveButton("확인", null)
                                .show();
                    }
                });
    }
}
