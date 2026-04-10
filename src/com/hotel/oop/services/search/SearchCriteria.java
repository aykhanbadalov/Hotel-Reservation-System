package com.hotel.oop.services.search;

import com.hotel.oop.models.room.RoomType;

import java.time.LocalDate;

/**
 * Value object describing what the guest (or UI) wants to search for.
 * Kept simple so {@link com.hotel.oop.models.interfaces.Searchable#searchByCriteria(SearchCriteria)} stays readable.
 */
public class SearchCriteria {

    private final String keyword;
    private final RoomType roomType;
    private final Integer minGuests;
    private final LocalDate availableFrom;
    private final LocalDate availableTo;

    public SearchCriteria(String keyword, RoomType roomType, Integer minGuests,
                          LocalDate availableFrom, LocalDate availableTo) {
        this.keyword = keyword;
        this.roomType = roomType;
        this.minGuests = minGuests;
        this.availableFrom = availableFrom;
        this.availableTo = availableTo;
    }

    public String getKeyword() {
        return keyword;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public Integer getMinGuests() {
        return minGuests;
    }

    public LocalDate getAvailableFrom() {
        return availableFrom;
    }

    public LocalDate getAvailableTo() {
        return availableTo;
    }

    public boolean hasAvailabilityWindow() {
        return availableFrom != null && availableTo != null;
    }
}
