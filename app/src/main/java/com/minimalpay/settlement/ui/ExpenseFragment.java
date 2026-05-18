package com.minimalpay.settlement.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.minimalpay.settlement.MinimalPayApp;
import com.minimalpay.settlement.R;
import com.minimalpay.settlement.control.StrategyFactory;
import com.minimalpay.settlement.domain.Expense;
import com.minimalpay.settlement.domain.Member;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * UC-2: 지출 등록 · 정산 방식
 */
public class ExpenseFragment extends Fragment {

    private SettlementSession session;
    private Spinner spinnerPayer;
    private final Set<String> selectedParticipantIds = new HashSet<>();
    private ParticipantAdapter participantAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_expense, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        session = MinimalPayApp.sessionFrom(requireActivity().getApplication());

        TextInputEditText editDesc = view.findViewById(R.id.editExpenseDesc);
        TextInputEditText editAmount = view.findViewById(R.id.editAmount);
        Spinner spinnerStrategy = view.findViewById(R.id.spinnerStrategy);
        spinnerPayer = view.findViewById(R.id.spinnerPayer);
        RecyclerView recycler = view.findViewById(R.id.recyclerParticipants);
        MaterialButton btnRegister = view.findViewById(R.id.btnRegisterExpense);

        ArrayAdapter<StrategyFactory.StrategyOption> strategyAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, StrategyFactory.availableOptions());
        strategyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStrategy.setAdapter(strategyAdapter);

        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        participantAdapter = new ParticipantAdapter();
        recycler.setAdapter(participantAdapter);

        btnRegister.setOnClickListener(v -> registerExpense(editDesc, editAmount, spinnerStrategy));

        refreshSpinners();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshSpinners();
        if (participantAdapter != null) {
            participantAdapter.notifyDataSetChanged();
        }
    }

    private void refreshSpinners() {
        ArrayAdapter<Member> payerAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, session.getMembers());
        payerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPayer.setAdapter(payerAdapter);
    }

    private void registerExpense(TextInputEditText editDesc, TextInputEditText editAmount,
                                 Spinner spinnerStrategy) {
        String amountText = textOf(editAmount);
        if (amountText.isEmpty()) {
            toast("금액을 입력하세요.");
            return;
        }
        final long amount;
        try {
            amount = Long.parseLong(amountText);
            if (amount <= 0) {
                toast("금액은 1원 이상이어야 합니다.");
                return;
            }
        } catch (NumberFormatException ex) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("입력 오류 (NumberFormatException)")
                    .setMessage("금액은 숫자(정수)만 입력할 수 있습니다.\n입력값: \"" + amountText + "\"")
                    .setPositiveButton("확인", null)
                    .show();
            return;
        }

        try {
            if (!session.getController().hasGroup()) {
                toast("먼저 [그룹] 탭에서 그룹을 생성하세요.");
                return;
            }
            Member payer = (Member) spinnerPayer.getSelectedItem();
            if (payer == null) {
                toast("결제자를 선택하세요.");
                return;
            }
            List<String> participantIds = new ArrayList<>(selectedParticipantIds);
            if (participantIds.isEmpty()) {
                toast("참여 멤버를 선택하세요.");
                return;
            }

            String desc = textOf(editDesc);
            if (desc.isEmpty()) {
                desc = "지출";
            }
            StrategyFactory.StrategyOption option =
                    (StrategyFactory.StrategyOption) spinnerStrategy.getSelectedItem();

            Expense exp = session.getController().registerExpense(
                    desc, amount, payer.getId(), participantIds, option.key);

            toast(String.format("등록: %s / %,d원 — [다음]으로 정산 단계로 이동할 수 있습니다", exp.getDescription(), exp.getTotalAmountMinor()));
            editDesc.setText("");
            editAmount.setText("");
            selectedParticipantIds.clear();
            participantAdapter.notifyDataSetChanged();
            notifyHost();
        } catch (Exception ex) {
            toast(ex.getMessage());
        }
    }

    private class ParticipantAdapter extends RecyclerView.Adapter<ParticipantAdapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            MaterialCheckBox box = (MaterialCheckBox) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_participant_check, parent, false);
            return new Holder(box);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            Member m = session.getMembers().get(position);
            holder.checkBox.setText(m.toString());
            holder.checkBox.setOnCheckedChangeListener(null);
            holder.checkBox.setChecked(selectedParticipantIds.contains(m.getId()));
            holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedParticipantIds.add(m.getId());
                } else {
                    selectedParticipantIds.remove(m.getId());
                }
            });
        }

        @Override
        public int getItemCount() {
            return session.getMembers().size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final MaterialCheckBox checkBox;

            Holder(MaterialCheckBox checkBox) {
                super(checkBox);
                this.checkBox = checkBox;
            }
        }
    }

    private static String textOf(TextInputEditText edit) {
        return edit.getText() != null ? edit.getText().toString().trim() : "";
    }

    private void notifyHost() {
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).refreshWizardState();
        }
    }

    private void toast(String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
