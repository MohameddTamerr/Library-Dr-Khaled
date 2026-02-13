package com.library.pos.model;

public enum PaymentMethod {
    CASH("Cash"),
    INSTAPAY("InstaPay"),
    VISA("Visa"),
    VODAFONE_CASH("Vodafone Cash");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
