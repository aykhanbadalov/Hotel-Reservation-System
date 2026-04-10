package com.hotel.oop.models.interfaces;

/**
 * <strong>INTERFACE #2 — {@code Payable}</strong> (course requirement).
 * <p>
 * <strong>Why an interface?</strong> Payment might later become “card”, “cash”, or “invoice”.
 * The rest of the app only calls {@link #processPayment(double)} — the implementation can evolve
 * without changing reservation orchestration (dependency inversion / open-closed idea).
 */
public interface Payable {

    /**
     * @param amount total to charge for the stay
     * @return {@code true} if payment succeeded in this simulation
     */
    boolean processPayment(double amount);
}
