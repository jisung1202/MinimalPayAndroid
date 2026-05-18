package com.minimalpay.settlement.control;

import android.content.Context;
import android.text.InputType;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

import com.minimalpay.settlement.R;
import com.minimalpay.settlement.domain.Member;

/**
 * UC-4 Extend: 계좌 부재 시 가이드 팝업 후 수동 입력.
 */
public class ManualAccountTransferLinker extends AppTransferLinker {

    @Override
    public void openTransfer(Context context, Member from, Member to, long amountMinor,
                             TransferCallback callback) {
        if (!to.hasBankAccount()) {
            new AlertDialog.Builder(context)
                    .setTitle("UC-4 Extend — 수동 입력 유도")
                    .setMessage(context.getString(R.string.extend_guide)
                            + "\n\n▶ 수신자: " + to.getName())
                    .setPositiveButton("계좌 입력", (d, w) -> showAccountInput(context, from, to, amountMinor, callback))
                    .setNegativeButton("취소", (d, w) -> callback.onFailure("계좌 입력이 취소되었습니다."))
                    .show();
            return;
        }
        super.openTransfer(context, from, to, amountMinor, callback);
    }

    private void showAccountInput(Context context, Member from, Member to, long amountMinor,
                                  TransferCallback callback) {
        EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("신한은행 110-123-456789");

        new AlertDialog.Builder(context)
                .setTitle("계좌 수동 입력")
                .setMessage(to.getName() + " 님의 은행/계좌번호")
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
                .setNegativeButton("취소", (d, w) -> callback.onFailure("계좌 입력이 취소되었습니다."))
                .show();
    }
}
