package com.hotel.oop.models.user;

/**
 * <strong>INHERITANCE:</strong> Hotel customer persona.
 */
public class Guest extends User {

    public Guest(long id, String fullName, String email) {
        super(id, fullName, email);
    }

    @Override
    public String getRole() {
        return "GUEST";
    }
}
