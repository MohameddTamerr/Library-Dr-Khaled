package com.library.pos.model;

import java.math.BigDecimal;

public class CustomerDeferredSummary {
    private final Customer customer;
    private final BigDecimal totalDeferred;
    private final BigDecimal totalPaid;
    private final BigDecimal balanceDue;
    private final PaymentStatus paymentStatus;

    public CustomerDeferredSummary(Customer customer, BigDecimal totalDeferred, BigDecimal totalPaid, BigDecimal balanceDue,
            PaymentStatus paymentStatus) {
        this.customer = customer;
        this.totalDeferred = totalDeferred;
        this.totalPaid = totalPaid;
        this.balanceDue = balanceDue;
        this.paymentStatus = paymentStatus;
    }

    public Customer getCustomer() {
        return customer;
    }

    public BigDecimal getTotalDeferred() {
        return totalDeferred;
    }

    public BigDecimal getTotalPaid() {
        return totalPaid;
    }

    public BigDecimal getBalanceDue() {
        return balanceDue;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }
}
