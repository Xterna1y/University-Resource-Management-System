package com.campus.client.model;

public class DiscussionRoom extends Facility {

    public DiscussionRoom(String resourceId,
                          String name,
                          String building,
                          int capacity) {
        super(resourceId, name, building, capacity);
    }

    @Override
    public String getType() {
        return "Discussion Room";
    }
}