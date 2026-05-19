package com.minimalpay.settlement.control;

import android.content.Context;

import com.minimalpay.settlement.domain.Member;

/**
 * GRASP Protected Variations: abstracts external transfer integration.
 */
public interface TransferLinker {
    void openTransfer(Context context, Member from, Member to, long amountMinor, TransferCallback callback);
}
