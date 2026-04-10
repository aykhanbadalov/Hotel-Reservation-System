package com.hotel.oop.models.user;

import java.util.Objects;

/**
 * <strong>ABSTRACT CLASS #2 — {@code User}</strong> (course requirement).
 * <p>
 * <strong>Why abstract?</strong> Guests and admins share contact data, but <em>authorization semantics</em> differ.
 * {@link #getRole()} is the hook subclasses must implement — classic inheritance + polymorphism.
 */
public abstract class User {

    private final long id;
    private String fullName;
    private String email;

    protected User(long id, String fullName, String email) {
        this.id = id;
        this.fullName = Objects.requireNonNull(fullName);
        this.email = Objects.requireNonNull(email);
    }

    public long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = Objects.requireNonNull(fullName);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = Objects.requireNonNull(email);
    }

    /**
     * Role discriminator for UI / future security (demo-level).
     */
    public abstract String getRole();

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        User user = (User) o;
        return id == user.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }
}
