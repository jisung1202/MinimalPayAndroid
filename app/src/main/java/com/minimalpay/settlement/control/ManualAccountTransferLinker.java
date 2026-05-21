package com.minimalpay.settlement.control;

import android.content.Context;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.appcompat.app.AlertDialog;

import com.minimalpay.settlement.R;
import com.minimalpay.settlement.domain.Member;

/**
 * UC-4 Extend: asks for bank and account information when the receiver has no account.
 */
public class ManualAccountTransferLinker extends AppTransferLinker {

    @Override
    public void openTransfer(Context context, Member from, Member to, long amountMinor,
                             TransferCallback callback) {
        if (!to.hasBankAccount()) {
            new AlertDialog.Builder(context)
                    .setTitle("계좌 정보 필요")
                    .setMessage(context.getString(R.string.extend_guide)
                            + "\n\n받는 사람: " + to.getName())
                    .setPositiveButton("계좌 입력", (dialog, which) ->
                            showAccountInput(context, from, to, amountMinor, callback))
                    .setNegativeButton("취소", (dialog, which) ->
                            callback.onFailure("계좌 입력이 취소되었습니다."))
                    .show();
            return;
        }
        super.openTransfer(context, from, to, amountMinor, callback);
    }

    private void showAccountInput(Context context, Member from, Member to, long amountMinor,
                                  TransferCallback callback) {
        LinearLayout form = new LinearLayout(context);
        form.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(20 * context.getResources().getDisplayMetrics().density);
        form.setPadding(padding, 0, padding, 0);

        Spinner bankSpinner = new Spinner(context);
        ArrayAdapter<String> bankAdapter = new ArrayAdapter<>(
                context, android.R.layout.simple_spinner_item, BankCatalog.BANK_NAMES);
        bankAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        bankSpinner.setAdapter(bankAdapter);
        form.addView(bankSpinner, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        EditText accountInput = new EditText(context);
        accountInput.setInputType(InputType.TYPE_CLASS_TEXT);
        accountInput.setHint("계좌번호");
        form.addView(accountInput, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        new AlertDialog.Builder(context)
                .setTitle("계좌 직접 입력")
                .setMessage(to.getName() + "님의 받을 계좌를 입력해 주세요.")
                .setView(form)
                .setPositiveButton("확인", (dialog, which) -> {
                    String accountNumber = accountInput.getText().toString().trim();
                    if (accountNumber.isEmpty()) {
                        callback.onFailure("계좌번호가 입력되지 않았습니다.");
                        return;
                    }
                    String bankName = (String) bankSpinner.getSelectedItem();
                    to.setBankAccount(BankCatalog.formatAccount(bankName, accountNumber));
                    super.openTransfer(context, from, to, amountMinor, callback);
                })
                .setNegativeButton("취소", (dialog, which) ->
                        callback.onFailure("계좌 입력이 취소되었습니다."))
                .show();
    }
}
