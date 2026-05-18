package com.minimalpay.settlement.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.minimalpay.settlement.R;
import com.minimalpay.settlement.domain.Member;

import java.util.List;

public class MemberListAdapter extends RecyclerView.Adapter<MemberListAdapter.Holder> {

    private final List<Member> members;

    public MemberListAdapter(List<Member> members) {
        this.members = members;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_member, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Member m = members.get(position);
        holder.index.setText(String.valueOf(position + 1));
        holder.name.setText(m.getName());
        holder.account.setText(m.hasBankAccount() ? m.getBankAccount() : "계좌 미등록");
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView index;
        final TextView name;
        final TextView account;

        Holder(@NonNull View itemView) {
            super(itemView);
            index = itemView.findViewById(R.id.textMemberIndex);
            name = itemView.findViewById(R.id.textMemberName);
            account = itemView.findViewById(R.id.textMemberAccount);
        }
    }
}
