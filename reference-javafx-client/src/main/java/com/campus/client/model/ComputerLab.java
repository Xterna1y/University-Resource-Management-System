package com.campus.client.model;

public class ComputerLab extends Facility {
    public ComputerLab(
            String resourceId,
            String name,
            String building,
            int capacity) {
        super(resourceId, name, building, capacity);
    }

    @Override
    public String getType() { return "Computer Lab"; }
}

