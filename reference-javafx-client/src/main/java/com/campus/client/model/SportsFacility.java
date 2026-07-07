package com.campus.client.model;

public class SportsFacility extends Facility {

    public SportsFacility(String resourceId,
                          String name,
                          String building,
                          int capacity) {
        super(resourceId, name, building, capacity);
    }

    @Override
    public String getType() {
        return "Sports Facility";
    }
}