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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.minimalpay.settlement.MinimalPayApp;
import com.minimalpay.settlement.R;
import com.minimalpay.settlement.domain.Member;

/**
 * UC-1: 그룹 카드 안에 참여 멤버 추가·목록을 포함.
 */
public class GroupFragment extends Fragment {

    private SettlementSession session;
    private MemberListAdapter adapter;

    private LinearLayout panelCreateGroup;
    private LinearLayout panelGroupDetail;
    private TextView textGroupNameDisplay;
    private TextView textMemberProgress;
    private TextView textMemberListTitle;
    private TextView textEmpty;
    private TextInputEditText editGroup;
    private TextInputEditText editMemberCount;
    private TextInputEditText editMemberName;
    private TextInputEditText editMemberAccount;
    private MaterialButton btnAddMember;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_group, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        session = MinimalPayApp.sessionFrom(requireActivity().getApplication());

        panelCreateGroup = view.findViewById(R.id.panelCreateGroup);
        panelGroupDetail = view.findViewById(R.id.panelGroupDetail);
        textGroupNameDisplay = view.findViewById(R.id.textGroupNameDisplay);
        textMemberProgress = view.findViewById(R.id.textMemberProgress);
        textMemberListTitle = view.findViewById(R.id.textMemberListTitle);
        textEmpty = view.findViewById(R.id.textEmptyMembers);

        editGroup = view.findViewById(R.id.editGroupName);
        editMemberCount = view.findViewById(R.id.editMemberCount);
        editMemberName = view.findViewById(R.id.editMemberName);
        editMemberAccount = view.findViewById(R.id.editMemberAccount);
        btnAddMember = view.findViewById(R.id.btnAddMember);

        RecyclerView recycler = view.findViewById(R.id.recyclerMembers);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new MemberListAdapter(session.getMembers());
        recycler.setAdapter(adapter);

        view.findViewById(R.id.btnCreateGroup).setOnClickListener(v -> onCreateGroup());
        btnAddMember.setOnClickListener(v -> onAddMember());

        refreshUi();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshUi();
    }

    private void onCreateGroup() {
        String name = textOf(editGroup);
        if (name.isEmpty()) {
            toast("그룹명을 입력하세요.");
            return;
        }

        String countText = textOf(editMemberCount);
        if (countText.isEmpty()) {
            toast("참여 인원 수를 입력하세요.");
            return;
        }

        final int expected;
        try {
            expected = Integer.parseInt(countText);
            if (expected < 2) {
                toast("인원은 2명 이상이어야 합니다.");
                return;
            }
        } catch (NumberFormatException ex) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("입력 오류")
                    .setMessage("인원 수는 숫자로 입력하세요.")
                    .setPositiveButton("확인", null)
                    .show();
            return;
        }

        session.getController().createGroup(name);
        session.setGroupName(name);
        session.setExpectedMemberCount(expected);
        session.clearMembers();
        adapter.notifyDataSetChanged();
        refreshUi();
        toast(String.format("「%s」 그룹이 생성되었습니다. 아래에서 멤버 %d명을 추가하세요.", name, expected));
        notifyHost();
    }

    private void onAddMember() {
        if (!session.getController().hasGroup()) {
            toast("먼저 그룹을 생성해 주세요.");
            return;
        }

        int expected = session.getExpectedMemberCount();
        if (session.getMembers().size() >= expected) {
            toast("이 그룹의 멤버 " + expected + "명이 모두 등록되었습니다.");
            return;
        }

        try {
            String name = textOf(editMemberName);
            if (name.isEmpty()) {
                toast("멤버 이름을 입력하세요.");
                return;
            }
            Member m = session.getController().addMember(name, textOf(editMemberAccount));
            session.addMember(m);
            adapter.notifyItemInserted(session.getMembers().size() - 1);
            editMemberName.setText("");
            editMemberAccount.setText("");
            refreshUi();

            if (session.isGroupStepComplete()) {
                toast("모든 참여 멤버가 등록되었습니다. [다음]을 눌러주세요.");
            }
            notifyHost();
        } catch (Exception ex) {
            toast(ex.getMessage());
        }
    }

    private void refreshUi() {
        boolean hasGroup = session.getController().hasGroup()
                && session.getGroupName() != null
                && !session.getGroupName().isBlank();

        panelCreateGroup.setVisibility(hasGroup ? View.GONE : View.VISIBLE);
        panelGroupDetail.setVisibility(hasGroup ? View.VISIBLE : View.GONE);

        if (!hasGroup) {
            return;
        }

        int expected = session.getExpectedMemberCount();
        int added = session.getMembers().size();

        textGroupNameDisplay.setText(session.getGroupName());
        textMemberProgress.setText(String.format("%d / %d명", added, expected));
        textMemberListTitle.setText(
                getString(R.string.label_member_list_in_group) + " (" + added + "명)");

        boolean full = added >= expected && expected > 0;
        textEmpty.setVisibility(added == 0 ? View.VISIBLE : View.GONE);
        btnAddMember.setEnabled(!full);
        btnAddMember.setAlpha(full ? 0.5f : 1f);

        editMemberName.setEnabled(!full);
        editMemberAccount.setEnabled(!full);
    }

    private void notifyHost() {
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).refreshWizardState();
        }
    }

    private static String textOf(TextInputEditText edit) {
        return edit.getText() != null ? edit.getText().toString().trim() : "";
    }

    private void toast(String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
