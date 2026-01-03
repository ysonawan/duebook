package com.duebook.app.model;

/**
 * Enum representing all possible audit actions in the application.
 * This enum is used to track and log all business transactions.
 */
public enum AuditAction {

    SHOP("Shop"),
    SHOP_CREATED("Shop Created"),
    SHOP_UPDATED("Shop Updated"),
    SHOP_USER_ADDED("Shop User Added"),
    SHOP_USER_REMOVED("Shop User Removed"),

    CUSTOMER("Customer"),
    CUSTOMER_CREATED("Customer Created"),
    CUSTOMER_UPDATED("Customer Updated"),

    LEDGER("Ledger"),
    LEDGER_ENTRY_CREATED("Ledger Entry Created"),
    LEDGER_REVERSAL("Ledger Reversal"),
    LEDGER_BALANCE_ADJUSTED("Ledger Balance Adjusted");

    private final String description;

    AuditAction(String description) {
        this.description = description;
    }

}

