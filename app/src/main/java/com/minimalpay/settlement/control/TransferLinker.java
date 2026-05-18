package com.minimalpay.settlement.control;

import android.content.Context;

import com.minimalpay.settlement.domain.Member;

/**
 * GRASP — Protected Variations: 외부 송금 연동 (UC-4).
 */
public interface TransferLinker {
    void openTransfer(Context context, Member from, Member to, long amountMinor, TransferCallback callback);
}
