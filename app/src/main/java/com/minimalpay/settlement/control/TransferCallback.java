package com.minimalpay.settlement.control;

public interface TransferCallback {
    void onSuccess();

    void onFailure(String message);
}
