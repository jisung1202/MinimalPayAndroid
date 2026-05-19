package com.minimalpay.settlement.control;

import android.content.Context;
import android.text.InputType;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

import com.minimalpay.settlement.R;
import com.minimalpay.settlement.domain.Member;

/**
 * UC-4 Extend: asks for account information when the receiver has no account.
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
        EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("예: 신한 110-123-456789");

        new AlertDialog.Builder(context)
                .setTitle("계좌 직접 입력")
                .setMessage(to.getName() + "님의 받을 계좌를 입력해 주세요.")
                .setView(input)
                .setPositiveButton("확인", (dialog, which) -> {
                    String text = input.getText().toString().trim();
                    if (text.isEmpty()) {
                        callback.onFailure("계좌 입력이 취소되었습니다.");
                        return;
                    }
                    to.setBankAccount(text);
                    super.openTransfer(context, from, to, amountMinor, callback);
                })
                .setNegativeButton("취소", (dialog, which) ->
                        callback.onFailure("계좌 입력이 취소되었습니다."))
                .show();
    }
}
