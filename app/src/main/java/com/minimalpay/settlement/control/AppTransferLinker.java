package com.minimalpay.settlement.control;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import com.minimalpay.settlement.domain.Member;

/**
 * Concrete transfer linker. This demo shows a confirmation dialog instead of
 * opening a real banking app.
 */
public class AppTransferLinker implements TransferLinker {

    @Override
    public void openTransfer(Context context, Member from, Member to, long amountMinor,
                             TransferCallback callback) {
        if (!to.hasBankAccount()) {
            callback.onFailure(to.getName() + "님의 계좌 정보가 없습니다.");
            return;
        }
        new AlertDialog.Builder(context)
                .setTitle("송금 확인")
                .setMessage(String.format(
                        "%s님이 %s님에게 %,d원을 송금합니다.\n계좌: %s",
                        from.getName(), to.getName(), amountMinor, to.getBankAccount()))
                .setPositiveButton("완료", (dialog, which) -> callback.onSuccess())
                .setNegativeButton("취소", null)
                .show();
    }
}
