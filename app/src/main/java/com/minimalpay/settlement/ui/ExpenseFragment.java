package com.minimalpay.settlement.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
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
import com.minimalpay.settlement.domain.EqualSplitStrategy;
import com.minimalpay.settlement.domain.Expense;
import com.minimalpay.settlement.domain.FixedAmountSplitStrategy;
import com.minimalpay.settlement.domain.ItemizedSplitStrategy;
import com.minimalpay.settlement.domain.Member;
import com.minimalpay.settlement.domain.SettlementStrategy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * UC-2: register expenses with the selected settlement strategy.
 */
public class ExpenseFragment extends Fragment {

    private SettlementSession session;
    private Spinner spinnerPayer;
    private Spinner spinnerFixedMember;
    private TextView textStrategyGuide;
    private View panelFixedAmount;
    private View panelItemized;
    private TextInputEditText editFixedAmount;
    private TextInputEditText editItemizedLines;
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
        spinnerFixedMember = view.findViewById(R.id.spinnerFixedMember);
        textStrategyGuide = view.findViewById(R.id.textStrategyGuide);
        panelFixedAmount = view.findViewById(R.id.panelFixedAmount);
        panelItemized = view.findViewById(R.id.panelItemized);
        editFixedAmount = view.findViewById(R.id.editFixedAmount);
        editItemizedLines = view.findViewById(R.id.editItemizedLines);

        RecyclerView recycler = view.findViewById(R.id.recyclerParticipants);
        MaterialButton btnRegister = view.findViewById(R.id.btnRegisterExpense);

        ArrayAdapter<StrategyFactory.StrategyOption> strategyAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, StrategyFactory.availableOptions());
        strategyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStrategy.setAdapter(strategyAdapter);
        spinnerStrategy.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View selectedView, int position, long id) {
                updateStrategyPanels((StrategyFactory.StrategyOption) parent.getItemAtPosition(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

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

        ArrayAdapter<Member> fixedAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, session.getMembers());
        fixedAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFixedMember.setAdapter(fixedAdapter);
    }

    private void updateStrategyPanels(StrategyFactory.StrategyOption option) {
        boolean fixed = StrategyFactory.KEY_FIXED_AMOUNT_SPLIT.equals(option.key);
        boolean itemized = StrategyFactory.KEY_ITEMIZED_SPLIT.equals(option.key);
        panelFixedAmount.setVisibility(fixed ? View.VISIBLE : View.GONE);
        panelItemized.setVisibility(itemized ? View.VISIBLE : View.GONE);

        if (fixed) {
            textStrategyGuide.setText("선택한 멤버는 고정 금액만 내고, 나머지는 다른 참여자가 1/N으로 나눕니다.");
        } else if (itemized) {
            textStrategyGuide.setText("품목별로 '품목명,금액,참여자1|참여자2' 형식으로 입력하세요. 품목 합계는 전체 금액과 같아야 합니다.");
        } else {
            textStrategyGuide.setText("선택한 참여 멤버가 전체 금액을 균등하게 나눕니다.");
        }
    }

    private void registerExpense(TextInputEditText editDesc, TextInputEditText editAmount,
                                 Spinner spinnerStrategy) {
        String amountText = textOf(editAmount);
        if (amountText.isEmpty()) {
            toast("금액을 입력해 주세요.");
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
                    .setTitle("입력 오류")
                    .setMessage("금액은 숫자만 입력할 수 있습니다.\n입력값: " + amountText)
                    .setPositiveButton("확인", null)
                    .show();
            return;
        }

        try {
            if (!session.getController().hasGroup()) {
                toast("먼저 [그룹] 탭에서 그룹을 생성해 주세요.");
                return;
            }
            Member payer = (Member) spinnerPayer.getSelectedItem();
            if (payer == null) {
                toast("결제자를 선택해 주세요.");
                return;
            }
            List<String> participantIds = new ArrayList<>(selectedParticipantIds);
            if (participantIds.isEmpty()) {
                toast("참여 멤버를 선택해 주세요.");
                return;
            }

            String desc = textOf(editDesc);
            if (desc.isEmpty()) {
                desc = "지출";
            }
            StrategyFactory.StrategyOption option =
                    (StrategyFactory.StrategyOption) spinnerStrategy.getSelectedItem();
            SettlementStrategy strategy = createStrategy(option, amount, participantIds);

            Expense expense = session.getController().registerExpense(
                    desc, amount, payer.getId(), participantIds, strategy);
            session.setLastReport(null);

            toast(String.format("등록 완료: %s / %,d원 / %s",
                    expense.getDescription(), expense.getTotalAmountMinor(), expense.getStrategyName()));
            editDesc.setText("");
            editAmount.setText("");
            editFixedAmount.setText("");
            editItemizedLines.setText("");
            selectedParticipantIds.clear();
            participantAdapter.notifyDataSetChanged();
            notifyHost();
        } catch (Exception ex) {
            toast(ex.getMessage());
        }
    }

    private SettlementStrategy createStrategy(StrategyFactory.StrategyOption option, long totalAmount,
                                              List<String> participantIds) {
        if (StrategyFactory.KEY_FIXED_AMOUNT_SPLIT.equals(option.key)) {
            Member fixedMember = (Member) spinnerFixedMember.getSelectedItem();
            if (fixedMember == null) {
                throw new IllegalArgumentException("고정 금액 대상자를 선택해 주세요.");
            }
            long fixedAmount = parsePositiveOrZero(textOf(editFixedAmount), "고정 금액");
            return new FixedAmountSplitStrategy(fixedMember.getId(), fixedAmount);
        }
        if (StrategyFactory.KEY_ITEMIZED_SPLIT.equals(option.key)) {
            return new ItemizedSplitStrategy(parseItemizedLines(textOf(editItemizedLines), participantIds));
        }
        return new EqualSplitStrategy();
    }

    private long parsePositiveOrZero(String text, String label) {
        if (text.isEmpty()) {
            throw new IllegalArgumentException(label + "을 입력해 주세요.");
        }
        try {
            long value = Long.parseLong(text);
            if (value < 0) {
                throw new IllegalArgumentException(label + "은 0원 이상이어야 합니다.");
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + "은 숫자로 입력해 주세요.");
        }
    }

    private List<ItemizedSplitStrategy.ItemShare> parseItemizedLines(String raw, List<String> participantIds) {
        if (raw.isEmpty()) {
            throw new IllegalArgumentException("품목 정산 내용을 입력해 주세요.");
        }

        Map<String, String> nameToId = new LinkedHashMap<>();
        for (Member member : session.getMembers()) {
            nameToId.put(member.getName(), member.getId());
        }
        Set<String> allowedIds = new HashSet<>(participantIds);
        List<ItemizedSplitStrategy.ItemShare> items = new ArrayList<>();

        String[] lines = raw.split("\\r?\\n");
        for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
            String line = lines[lineIndex].trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split(",");
            if (parts.length != 3) {
                throw new IllegalArgumentException((lineIndex + 1) + "번째 줄 형식이 올바르지 않습니다.");
            }
            String itemName = parts[0].trim();
            long amount = Long.parseLong(parts[1].trim());
            List<String> itemMemberIds = new ArrayList<>();
            for (String name : parts[2].split("\\|")) {
                String memberId = nameToId.get(name.trim());
                if (memberId == null) {
                    throw new IllegalArgumentException("등록되지 않은 멤버 이름입니다: " + name.trim());
                }
                if (!allowedIds.contains(memberId)) {
                    throw new IllegalArgumentException("품목 참여자는 전체 참여 멤버에 포함되어야 합니다: " + name.trim());
                }
                itemMemberIds.add(memberId);
            }
            items.add(new ItemizedSplitStrategy.ItemShare(itemName, amount, itemMemberIds));
        }
        return items;
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
            Member member = session.getMembers().get(position);
            holder.checkBox.setText(member.toString());
            holder.checkBox.setOnCheckedChangeListener(null);
            holder.checkBox.setChecked(selectedParticipantIds.contains(member.getId()));
            holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedParticipantIds.add(member.getId());
                } else {
                    selectedParticipantIds.remove(member.getId());
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
