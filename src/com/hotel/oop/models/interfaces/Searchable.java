package com.hotel.oop.models.interfaces;

import com.hotel.oop.models.room.Room;
import com.hotel.oop.services.search.SearchCriteria;

import java.util.List;

/**
 * <strong>INTERFACE #3 — {@code Searchable}</strong> (course requirement).
 * <p>
 * <strong>Why an interface?</strong> Search could be backed by a list today and a database tomorrow.
 * The HTTP layer depends on {@code Searchable}, not on a specific storage class — again polymorphism + flexibility.
 */
public interface Searchable {

    /**
     * Returns rooms matching the given criteria (and optional date availability when dates are present).
     */
    List<Room> searchByCriteria(SearchCriteria criteria);
}
