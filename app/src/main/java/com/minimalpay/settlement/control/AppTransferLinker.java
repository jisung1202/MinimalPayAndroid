package com.minimalpay.settlement.control;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.appcompat.app.AlertDialog;

import com.minimalpay.settlement.domain.Member;

/**
 * Creates and opens a transfer deep link. If no app handles the link, it falls
 * back to Android share so the transfer payload is still usable.
 */
public class AppTransferLinker implements TransferLinker {

    @Override
    public void openTransfer(Context context, Member from, Member to, long amountMinor,
                             TransferCallback callback) {
        if (!to.hasBankAccount()) {
            callback.onFailure(to.getName() + "님의 계좌 정보가 없습니다.");
            return;
        }

        Uri deepLink = TransferDeepLinkBuilder.build(from, to, amountMinor);
        String message = String.format(
                "%s님이 %s님에게 %,d원을 송금합니다.\n계좌: %s\n\n딥링크\n%s\n\n생성한 딥링크는 정산을 해야 하는 인원에게 전송해 주세요.",
                from.getName(), to.getName(), amountMinor, to.getBankAccount(), deepLink);

        new AlertDialog.Builder(context)
                .setTitle("계좌 송금 딥링크")
                .setMessage(message)
                .setPositiveButton("송금 앱 열기", (dialog, which) -> openDeepLink(context, deepLink, callback))
                .setNeutralButton("링크 공유", (dialog, which) -> shareDeepLink(context, deepLink, callback))
                .setNegativeButton("수동 완료", (dialog, which) -> callback.onSuccess())
                .show();
    }

    private void openDeepLink(Context context, Uri deepLink, TransferCallback callback) {
        Intent intent = new Intent(Intent.ACTION_VIEW, deepLink);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
            callback.onSuccess();
        } catch (ActivityNotFoundException ex) {
            shareDeepLink(context, deepLink, callback);
        }
    }

    private void shareDeepLink(Context context, Uri deepLink, TransferCallback callback) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, deepLink.toString());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(Intent.createChooser(intent, "송금 딥링크 공유"));
        callback.onSuccess();
    }
}
