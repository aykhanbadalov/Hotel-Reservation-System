package com.hotel.oop.services;

import com.hotel.oop.models.interfaces.Payable;

import java.util.logging.Logger;

/**
 * <strong>Payable implementation</strong> — simulates a payment gateway.
 * <p>
 * The reservation workflow depends on {@link Payable}, not this concrete class (interface-driven design).
 */
public class PaymentService implements Payable {

    private static final Logger LOG = Logger.getLogger(PaymentService.class.getName());

    @Override
    public boolean processPayment(double amount) {
        if (amount <= 0) {
            return false;
        }
        LOG.info(() -> String.format("[PAYMENT SIMULATOR] Charged $%.2f successfully.", amount));
        return true;
    }
}
