package com.minimalpay.settlement.control;

public final class BankCatalog {
    public static final String[] BANK_NAMES = {
            "KB국민은행",
            "신한은행",
            "하나은행",
            "우리은행",
            "NH농협은행 (농협중앙회)",
            "IBK기업은행",
            "Sh수협은행",
            "KDB산업은행",
            "카카오뱅크",
            "토스뱅크",
            "케이뱅크",
            "우체국 (우체국예금)",
            "새마을금고",
            "신협 (신용협동조합)",
            "SC제일은행",
            "부산은행",
            "대구은행(iM뱅크)",
            "광주은행"
    };

    private BankCatalog() {
    }

    public static String formatAccount(String bankName, String accountNumber) {
        return bankName + " " + accountNumber.trim();
    }
}
