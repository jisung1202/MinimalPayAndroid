package com.minimalpay.settlement.ui;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.minimalpay.settlement.MinimalPayApp;
import com.minimalpay.settlement.R;
import com.minimalpay.settlement.control.SettlementController;

/**
 * UC-3: requestSettlementReport().
 */
public class ReportFragment extends Fragment {

    private SettlementSession session;
    private TextView textReport;

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
        textReport = view.findViewById(R.id.textReport);
        textReport.setTypeface(Typeface.MONOSPACE);

        MaterialButton btnReport = view.findViewById(R.id.btnReport);
        btnReport.setOnClickListener(v -> onRequestReport());
    }

    private void onRequestReport() {
        try {
            SettlementController.SettlementReport report =
                    session.getController().requestSettlementReport();
            session.setLastReport(report);
            textReport.setText(report.formatAsText());
            Toast.makeText(requireContext(), "정산 리포트를 생성했습니다.", Toast.LENGTH_SHORT).show();
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).refreshWizardState();
            }
        } catch (Exception ex) {
            Toast.makeText(requireContext(), ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
