package com.minimalpay.settlement.domain;

/**
 * GRASP — Information Expert: 멤버 고유 정보를 보유한다.
 */
public class Member {
    private final String id;
    private String name;
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
        return name + " [계좌없음]";
    }
}
