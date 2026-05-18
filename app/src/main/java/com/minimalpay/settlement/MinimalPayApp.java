package com.minimalpay.settlement;

import android.app.Application;

import com.minimalpay.settlement.ui.SettlementSession;

public class MinimalPayApp extends Application {

    private SettlementSession session;

    @Override
    public void onCreate() {
        super.onCreate();
        session = new SettlementSession();
    }

    public SettlementSession getSession() {
        return session;
    }

    public static SettlementSession sessionFrom(Application app) {
        return ((MinimalPayApp) app).getSession();
    }
}
