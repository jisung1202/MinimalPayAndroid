package com.minimalpay.settlement.ui;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
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
import com.google.android.material.textfield.TextInputLayout;
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

public class ExpenseFragment extends Fragment {

    private SettlementSession session;
    private TextInputEditText editDesc;
    private TextInputEditText editAmount;
    private Spinner spinnerStrategy;
    private Spinner spinnerPayer;
    private TextView textStrategyGuide;
    private View panelFixedAmount;
    private View panelItemized;
    private LinearLayout containerFixedRows;
    private LinearLayout containerItemRows;
    private LinearLayout containerRegisteredExpenses;
    private TextView textExpenseCount;
    private TextView textExpenseEmpty;
    private final List<FixedAmountRow> fixedRows = new ArrayList<>();
    private final List<ItemRow> itemRows = new ArrayList<>();
    private final Set<String> selectedParticipantIds = new HashSet<>();
    private ParticipantAdapter participantAdapter;
    private boolean restoringDraft;

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

        editDesc = view.findViewById(R.id.editExpenseDesc);
        editAmount = view.findViewById(R.id.editAmount);
        spinnerStrategy = view.findViewById(R.id.spinnerStrategy);
        spinnerPayer = view.findViewById(R.id.spinnerPayer);
        textStrategyGuide = view.findViewById(R.id.textStrategyGuide);
        panelFixedAmount = view.findViewById(R.id.panelFixedAmount);
        panelItemized = view.findViewById(R.id.panelItemized);
        containerFixedRows = view.findViewById(R.id.containerFixedRows);
        containerItemRows = view.findViewById(R.id.containerItemRows);
        containerRegisteredExpenses = view.findViewById(R.id.containerRegisteredExpenses);
        textExpenseCount = view.findViewById(R.id.textExpenseCount);
        textExpenseEmpty = view.findViewById(R.id.textExpenseEmpty);

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

        RecyclerView recycler = view.findViewById(R.id.recyclerParticipants);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        participantAdapter = new ParticipantAdapter();
        recycler.setAdapter(participantAdapter);

        view.findViewById(R.id.btnAddFixedAmount).setOnClickListener(v -> addFixedAmountRow(null));
        view.findViewById(R.id.btnAddItem).setOnClickListener(v -> addItemRow(null));
        view.findViewById(R.id.btnRegisterExpense).setOnClickListener(v -> registerExpense());

        refreshSpinners();
        restoreDraft();
        renderRegisteredExpenses();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshSpinners();
        participantAdapter.notifyDataSetChanged();
        renderRegisteredExpenses();
    }

    @Override
    public void onPause() {
        super.onPause();
        saveDraft();
    }

    private void refreshSpinners() {
        String selectedPayerId = selectedMemberId(spinnerPayer);
        ArrayAdapter<Member> payerAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, session.getMembers());
        payerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPayer.setAdapter(payerAdapter);
        selectMember(spinnerPayer, selectedPayerId);
        for (FixedAmountRow row : fixedRows) {
            row.refreshMembers();
        }
    }

    private void updateStrategyPanels(StrategyFactory.StrategyOption option) {
        boolean fixed = StrategyFactory.KEY_FIXED_AMOUNT_SPLIT.equals(option.key);
        boolean itemized = StrategyFactory.KEY_ITEMIZED_SPLIT.equals(option.key);
        panelFixedAmount.setVisibility(fixed ? View.VISIBLE : View.GONE);
        panelItemized.setVisibility(itemized ? View.VISIBLE : View.GONE);
        if (fixed) {
            textStrategyGuide.setText("고정 금액 대상은 정산 참여자에 자동 포함됩니다. 남은 금액은 나머지 참여자가 1/N으로 나눕니다.");
            if (!restoringDraft && fixedRows.isEmpty()) {
                addFixedAmountRow(null);
            }
        } else if (itemized) {
            textStrategyGuide.setText("품목 추가 버튼으로 박스를 만들고, 품목마다 이름, 금액, 참여자를 따로 입력하세요.");
            if (!restoringDraft && itemRows.isEmpty()) {
                addItemRow(null);
            }
        } else {
            textStrategyGuide.setText("선택한 참여 멤버가 전체 금액을 균등하게 나눕니다.");
        }
    }

    private void registerExpense() {
        long amount;
        try {
            amount = parsePositive(textOf(editAmount), "금액");
        } catch (IllegalArgumentException ex) {
            showInputError(ex.getMessage());
            return;
        }

        try {
            if (!session.getController().hasGroup()) {
                toast("먼저 [그룹] 화면에서 그룹을 생성해 주세요.");
                return;
            }
            Member payer = (Member) spinnerPayer.getSelectedItem();
            if (payer == null) {
                toast("결제자를 선택해 주세요.");
                return;
            }

            StrategyFactory.StrategyOption option =
                    (StrategyFactory.StrategyOption) spinnerStrategy.getSelectedItem();
            List<String> participantIds = new ArrayList<>(selectedParticipantIds);
            if (StrategyFactory.KEY_FIXED_AMOUNT_SPLIT.equals(option.key)) {
                appendFixedMembersToParticipants(participantIds);
            }
            if (participantIds.isEmpty()) {
                toast("참여 멤버를 선택해 주세요.");
                return;
            }

            String desc = textOf(editDesc);
            if (desc.isEmpty()) {
                desc = "지출";
            }

            SettlementStrategy strategy = createStrategy(option, participantIds);
            Expense expense = session.getController().registerExpense(
                    desc, amount, payer.getId(), participantIds, strategy);
            session.setLastReport(null);

            toast(String.format("목록에 추가: %s / %,d원", expense.getDescription(), expense.getTotalAmountMinor()));
            clearDraftAndForm(option);
            renderRegisteredExpenses();
            notifyHost();
        } catch (Exception ex) {
            toast(ex.getMessage());
        }
    }

    private SettlementStrategy createStrategy(StrategyFactory.StrategyOption option,
                                              List<String> participantIds) {
        if (StrategyFactory.KEY_FIXED_AMOUNT_SPLIT.equals(option.key)) {
            return new FixedAmountSplitStrategy(collectFixedAmounts());
        }
        if (StrategyFactory.KEY_ITEMIZED_SPLIT.equals(option.key)) {
            return new ItemizedSplitStrategy(collectItems(participantIds));
        }
        return new EqualSplitStrategy();
    }

    private void appendFixedMembersToParticipants(List<String> participantIds) {
        for (FixedAmountRow row : fixedRows) {
            Member member = (Member) row.memberSpinner.getSelectedItem();
            if (member != null && !participantIds.contains(member.getId())) {
                participantIds.add(member.getId());
            }
        }
    }

    private Map<String, Long> collectFixedAmounts() {
        if (fixedRows.isEmpty()) {
            throw new IllegalArgumentException("고정 금액 대상자를 추가해 주세요.");
        }
        Map<String, Long> fixedAmounts = new LinkedHashMap<>();
        for (FixedAmountRow row : fixedRows) {
            Member member = (Member) row.memberSpinner.getSelectedItem();
            if (member == null) {
                throw new IllegalArgumentException("고정 금액 대상자를 선택해 주세요.");
            }
            if (fixedAmounts.containsKey(member.getId())) {
                throw new IllegalArgumentException("같은 멤버를 고정 금액 대상으로 중복 추가할 수 없습니다.");
            }
            fixedAmounts.put(member.getId(), parsePositiveOrZero(textOf(row.amountEdit), "고정 금액"));
        }
        return fixedAmounts;
    }

    private List<ItemizedSplitStrategy.ItemShare> collectItems(List<String> participantIds) {
        if (itemRows.isEmpty()) {
            throw new IllegalArgumentException("품목을 1개 이상 추가해 주세요.");
        }
        Set<String> participantSet = new HashSet<>(participantIds);
        List<ItemizedSplitStrategy.ItemShare> items = new ArrayList<>();
        for (ItemRow row : itemRows) {
            long amount = parsePositive(textOf(row.amountEdit), "품목 금액");
            List<String> itemMemberIds = row.checkedMemberIds();
            for (String memberId : itemMemberIds) {
                if (!participantSet.contains(memberId)) {
                    throw new IllegalArgumentException("품목 참여자는 정산 참여 멤버에도 체크되어야 합니다.");
                }
            }
            items.add(new ItemizedSplitStrategy.ItemShare(textOf(row.nameEdit), amount, itemMemberIds));
        }
        return items;
    }

    private long parsePositive(String text, String label) {
        long value = parsePositiveOrZero(text, label);
        if (value <= 0) {
            throw new IllegalArgumentException(label + "은 1원 이상이어야 합니다.");
        }
        return value;
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

    private void showInputError(String message) {
        new AlertDialog.Builder(requireContext())
                .setTitle("입력 오류")
                .setMessage(message)
                .setPositiveButton("확인", null)
                .show();
    }

    private void saveDraft() {
        if (session == null || editDesc == null) {
            return;
        }
        SettlementSession.ExpenseDraft draft = new SettlementSession.ExpenseDraft();
        draft.description = textOf(editDesc);
        draft.amount = textOf(editAmount);
        StrategyFactory.StrategyOption option = (StrategyFactory.StrategyOption) spinnerStrategy.getSelectedItem();
        draft.strategyKey = option != null ? option.key : "";
        draft.payerMemberId = selectedMemberId(spinnerPayer);
        draft.participantIds = new HashSet<>(selectedParticipantIds);
        for (FixedAmountRow row : fixedRows) {
            SettlementSession.FixedDraft fixedDraft = new SettlementSession.FixedDraft();
            fixedDraft.memberId = selectedMemberId(row.memberSpinner);
            fixedDraft.amount = textOf(row.amountEdit);
            draft.fixedRows.add(fixedDraft);
        }
        for (ItemRow row : itemRows) {
            SettlementSession.ItemDraft itemDraft = new SettlementSession.ItemDraft();
            itemDraft.name = textOf(row.nameEdit);
            itemDraft.amount = textOf(row.amountEdit);
            itemDraft.participantIds = new HashSet<>(row.checkedMemberIds());
            draft.itemRows.add(itemDraft);
        }
        session.setExpenseDraft(draft);
    }

    private void restoreDraft() {
        SettlementSession.ExpenseDraft draft = session.getExpenseDraft();
        restoringDraft = true;
        editDesc.setText(draft.description);
        editAmount.setText(draft.amount);
        selectStrategy(draft.strategyKey);
        selectMember(spinnerPayer, draft.payerMemberId);
        selectedParticipantIds.clear();
        selectedParticipantIds.addAll(draft.participantIds);
        containerFixedRows.removeAllViews();
        fixedRows.clear();
        for (SettlementSession.FixedDraft fixedDraft : draft.fixedRows) {
            addFixedAmountRow(fixedDraft);
        }
        containerItemRows.removeAllViews();
        itemRows.clear();
        for (SettlementSession.ItemDraft itemDraft : draft.itemRows) {
            addItemRow(itemDraft);
        }
        restoringDraft = false;
        updateStrategyPanels((StrategyFactory.StrategyOption) spinnerStrategy.getSelectedItem());
        participantAdapter.notifyDataSetChanged();
    }

    private void clearDraftAndForm(StrategyFactory.StrategyOption option) {
        editDesc.setText("");
        editAmount.setText("");
        selectedParticipantIds.clear();
        participantAdapter.notifyDataSetChanged();
        containerFixedRows.removeAllViews();
        containerItemRows.removeAllViews();
        fixedRows.clear();
        itemRows.clear();
        if (StrategyFactory.KEY_FIXED_AMOUNT_SPLIT.equals(option.key)) {
            addFixedAmountRow(null);
        } else if (StrategyFactory.KEY_ITEMIZED_SPLIT.equals(option.key)) {
            addItemRow(null);
        }
        session.setExpenseDraft(new SettlementSession.ExpenseDraft());
    }

    private void addFixedAmountRow(@Nullable SettlementSession.FixedDraft draft) {
        FixedAmountRow row = new FixedAmountRow();
        fixedRows.add(row);
        containerFixedRows.addView(row.root);
        if (draft != null) {
            row.amountEdit.setText(draft.amount);
            selectMember(row.memberSpinner, draft.memberId);
        }
    }

    private void addItemRow(@Nullable SettlementSession.ItemDraft draft) {
        ItemRow row = new ItemRow();
        itemRows.add(row);
        containerItemRows.addView(row.root);
        if (draft != null) {
            row.nameEdit.setText(draft.name);
            row.amountEdit.setText(draft.amount);
            row.setCheckedMemberIds(draft.participantIds);
        }
    }

    private TextInputEditText addInput(LinearLayout parent, String hint, int inputType) {
        TextInputLayout layout = new TextInputLayout(requireContext());
        layout.setHint(hint);
        layout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        layout.setBoxCornerRadii(dp(8), dp(8), dp(8), dp(8));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.topMargin = dp(8);
        parent.addView(layout, layoutParams);

        TextInputEditText edit = new TextInputEditText(requireContext());
        edit.setInputType(inputType);
        edit.setSingleLine(true);
        edit.setSelectAllOnFocus(true);
        layout.addView(edit, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return edit;
    }

    private LinearLayout createRowContainer() {
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundResource(R.drawable.bg_member_inner);
        root.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(8);
        root.setLayoutParams(params);
        return root;
    }

    private MaterialButton createDeleteButton(String text) {
        MaterialButton button = new MaterialButton(requireContext());
        button.setText(text);
        button.setCornerRadius(dp(8));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(8);
        button.setLayoutParams(params);
        return button;
    }

    private void renderRegisteredExpenses() {
        if (containerRegisteredExpenses == null || !session.getController().hasGroup()) {
            return;
        }
        containerRegisteredExpenses.removeAllViews();
        List<Expense> expenses = session.getController().getExpenses();
        textExpenseCount.setText(expenses.size() + "건");
        textExpenseEmpty.setVisibility(expenses.isEmpty() ? View.VISIBLE : View.GONE);
        for (Expense expense : expenses) {
            containerRegisteredExpenses.addView(createExpenseCard(expense));
        }
    }

    private View createExpenseCard(Expense expense) {
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setGravity(android.view.Gravity.CENTER_VERTICAL);
        root.setBackgroundResource(R.drawable.bg_member_inner);
        root.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams rootParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rootParams.bottomMargin = dp(8);
        root.setLayoutParams(rootParams);

        LinearLayout textGroup = new LinearLayout(requireContext());
        textGroup.setOrientation(LinearLayout.VERTICAL);
        root.addView(textGroup, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = new TextView(requireContext());
        title.setText(expense.getDescription());
        title.setTextColor(getResources().getColor(R.color.mp_text_primary, null));
        title.setTextSize(15);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        textGroup.addView(title);

        TextView meta = new TextView(requireContext());
        meta.setText(expense.getPayer().getName() + " 결제 · " + expense.getStrategyName());
        meta.setTextColor(getResources().getColor(R.color.mp_text_secondary, null));
        meta.setTextSize(12);
        textGroup.addView(meta);

        TextView amount = new TextView(requireContext());
        amount.setText(String.format("%,d원", expense.getTotalAmountMinor()));
        amount.setTextColor(getResources().getColor(R.color.mp_primary, null));
        amount.setTextSize(15);
        amount.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(amount);
        return root;
    }

    private void selectStrategy(String strategyKey) {
        if (strategyKey == null || strategyKey.isEmpty()) {
            return;
        }
        for (int i = 0; i < spinnerStrategy.getCount(); i++) {
            StrategyFactory.StrategyOption option =
                    (StrategyFactory.StrategyOption) spinnerStrategy.getItemAtPosition(i);
            if (strategyKey.equals(option.key)) {
                spinnerStrategy.setSelection(i);
                return;
            }
        }
    }

    private void selectMember(Spinner spinner, String memberId) {
        if (memberId == null || memberId.isEmpty() || spinner == null) {
            return;
        }
        for (int i = 0; i < spinner.getCount(); i++) {
            Member member = (Member) spinner.getItemAtPosition(i);
            if (memberId.equals(member.getId())) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private String selectedMemberId(Spinner spinner) {
        if (spinner == null || spinner.getSelectedItem() == null) {
            return "";
        }
        return ((Member) spinner.getSelectedItem()).getId();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private class FixedAmountRow {
        final LinearLayout root;
        final Spinner memberSpinner;
        final TextInputEditText amountEdit;

        FixedAmountRow() {
            root = createRowContainer();
            memberSpinner = new Spinner(requireContext());
            root.addView(memberSpinner, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
            amountEdit = addInput(root, "고정 금액 (원)", InputType.TYPE_CLASS_NUMBER);
            MaterialButton removeButton = createDeleteButton("대상 삭제");
            removeButton.setOnClickListener(v -> {
                fixedRows.remove(this);
                containerFixedRows.removeView(root);
            });
            root.addView(removeButton);
            refreshMembers();
        }

        void refreshMembers() {
            String selectedId = selectedMemberId(memberSpinner);
            ArrayAdapter<Member> adapter = new ArrayAdapter<>(
                    requireContext(), android.R.layout.simple_spinner_item, session.getMembers());
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            memberSpinner.setAdapter(adapter);
            selectMember(memberSpinner, selectedId);
        }
    }

    private class ItemRow {
        final LinearLayout root;
        final TextInputEditText nameEdit;
        final TextInputEditText amountEdit;
        final List<MaterialCheckBox> memberChecks = new ArrayList<>();

        ItemRow() {
            root = createRowContainer();
            nameEdit = addInput(root, "품목명", InputType.TYPE_CLASS_TEXT);
            amountEdit = addInput(root, "품목 금액 (원)", InputType.TYPE_CLASS_NUMBER);

            TextView memberLabel = new TextView(requireContext());
            memberLabel.setText("품목 참여자");
            memberLabel.setTextColor(getResources().getColor(R.color.mp_text_secondary, null));
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            labelParams.topMargin = dp(8);
            root.addView(memberLabel, labelParams);

            for (Member member : session.getMembers()) {
                MaterialCheckBox checkBox = new MaterialCheckBox(requireContext());
                checkBox.setText(member.getName());
                checkBox.setTag(member.getId());
                root.addView(checkBox);
                memberChecks.add(checkBox);
            }

            MaterialButton removeButton = createDeleteButton("품목 삭제");
            removeButton.setOnClickListener(v -> {
                itemRows.remove(this);
                containerItemRows.removeView(root);
            });
            root.addView(removeButton);
        }

        List<String> checkedMemberIds() {
            List<String> ids = new ArrayList<>();
            for (MaterialCheckBox checkBox : memberChecks) {
                if (checkBox.isChecked()) {
                    ids.add((String) checkBox.getTag());
                }
            }
            return ids;
        }

        void setCheckedMemberIds(Set<String> memberIds) {
            for (MaterialCheckBox checkBox : memberChecks) {
                checkBox.setChecked(memberIds.contains((String) checkBox.getTag()));
            }
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
                saveDraft();
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
