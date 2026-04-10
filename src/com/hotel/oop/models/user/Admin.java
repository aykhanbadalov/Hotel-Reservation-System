package com.hotel.oop.models.user;

/**
 * <strong>INHERITANCE:</strong> Staff persona — same {@link User} abstraction, different {@link #getRole()}.
 */
public class Admin extends User {

    public Admin(long id, String fullName, String email) {
        super(id, fullName, email);
    }

    @Override
    public String getRole() {
        return "ADMIN";
    }
}
