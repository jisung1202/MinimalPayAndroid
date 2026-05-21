package com.minimalpay.settlement.control;

import android.net.Uri;

import com.minimalpay.settlement.domain.Member;

/**
 * Creates a neutral transfer deep link. Banking apps can map this URI shape
 * later, while the current app can still share or open it through Android.
 */
public final class TransferDeepLinkBuilder {
    private TransferDeepLinkBuilder() {
    }

    public static Uri build(Member from, Member to, long amountMinor) {
        return new Uri.Builder()
                .scheme("minimalpay")
                .authority("transfer")
                .appendQueryParameter("fromName", from.getName())
                .appendQueryParameter("toName", to.getName())
                .appendQueryParameter("account", to.getBankAccount())
                .appendQueryParameter("amount", String.valueOf(amountMinor))
                .appendQueryParameter("currency", "KRW")
                .build();
    }
}
