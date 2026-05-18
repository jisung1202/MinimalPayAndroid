package com.minimalpay.settlement.control;

public class MissingAccountInfoException extends Exception {
    public MissingAccountInfoException(String message) {
        super(message);
    }
}
