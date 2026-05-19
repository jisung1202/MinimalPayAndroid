package com.minimalpay.settlement.domain;

/**
 * GRASP Information Expert: owns member identity, account, and balance state.
 */
public class Member {
    private final String id;
    private final String name;
    private String bankAccount;
    private long balanceMinor;

    public Member(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getBankAccount() {
        return bankAccount;
    }

    public void setBankAccount(String bankAccount) {
        if (bankAccount == null || bankAccount.trim().isEmpty()) {
            this.bankAccount = null;
        } else {
            this.bankAccount = bankAccount.trim();
        }
    }

    public boolean hasBankAccount() {
        return bankAccount != null && !bankAccount.isEmpty();
    }

    public long getBalanceMinor() {
        return balanceMinor;
    }

    public void addBalanceMinor(long delta) {
        balanceMinor += delta;
    }

    @Override
    public String toString() {
        if (hasBankAccount()) {
            return name + " (" + bankAccount + ")";
        }
        return name + " [계좌 없음]";
    }
}
