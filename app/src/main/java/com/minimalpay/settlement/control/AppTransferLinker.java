package com.minimalpay.settlement.control;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import com.minimalpay.settlement.domain.Member;

/**
 * GRASP — Concrete Class: 앱 연동(계좌 있을 때).
 */
public class AppTransferLinker implements TransferLinker {

    @Override
    public void openTransfer(Context context, Member from, Member to, long amountMinor,
                             TransferCallback callback) {
        if (!to.hasBankAccount()) {
            callback.onFailure(to.getName() + " 님의 계좌 정보가 없습니다.");
            return;
        }
        new AlertDialog.Builder(context)
                .setTitle("외부 송금")
                .setMessage(String.format(
                        "[MinimalPay 앱 연동 완료]\n%s → %s\n%,d원 → %s",
                        from.getName(), to.getName(), amountMinor, to.getBankAccount()))
                .setPositiveButton("확인", (d, w) -> callback.onSuccess())
                .show();
    }
}
